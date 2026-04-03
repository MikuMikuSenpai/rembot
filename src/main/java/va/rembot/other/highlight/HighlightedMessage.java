package va.rembot.other.highlight;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import va.rembot.BotConfig;
import va.rembot.database.dao.MessageDao;
import va.rembot.database.dao.StarMessageDao;
import va.rembot.database.models.StarMessage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

@Slf4j
public class HighlightedMessage extends ListenerAdapter {

    //TODO make exceptions and add them at the orelsethrow, cba atm
    //TODO its possible to edit the msghighlight bs
    // if we also give the amount of stars using MessageChannel. editEmbedmsg (sum like this) we prob wont do this?ask miku
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {

        var emoji = event.getEmoji().asUnicode();

        //only do our bs if its an actual star
        if (emoji.getAsCodepoints().equalsIgnoreCase("U+2B50")) {

            var msgId = event.getMessageIdLong();
            var starMsgDao = new StarMessageDao();
            var msgDao = new MessageDao();
            starMsgDao.create(new StarMessage(msgId, 0, false));//init the bs thing

            var starAmount = 0;
            starAmount = starMsgDao.get(msgId)
                    .orElseThrow()
                    .starAmount();

            var isSent = starMsgDao.get(msgId).orElseThrow().isSent();

            var newStarAmount = starAmount + 1;

            // update w new star amoutn
            starMsgDao.update(new StarMessage(msgId, newStarAmount, isSent));

            var msgContent = msgDao.get(msgId)
                   .orElseThrow()
                   .messageContent();

            var msgAuthorId = msgDao.get(msgId)
                    .orElseThrow()
                    .discordId();

            var user = event.getJDA().getUserById(msgAuthorId);

            //notttgointolie no fucking idea why we have to check the msgcontent
            // for it to not be empty forgot how i stumbled onto this ig keep it since it doesnt hurt,
            // issent is to prevent dupes/duplicate msg highlights in darwin
            if (newStarAmount >= BotConfig.getHighlightStarThresholdInt() && !msgContent.isEmpty() && !isSent){

                isSent = true;
                //TODO we should make a dedicated method for setting isSent to true cus this is sloppy:
                // after some consideration it isnt that bad ill think more about it in the real PR
                starMsgDao.update(new StarMessage(msgId, newStarAmount, isSent));

                //TODO ask miku how he wants the frontend
                EmbedBuilder embedHighlight = new EmbedBuilder();
                embedHighlight.setTitle("⭐ message highlight ⭐");
                embedHighlight.addField("user", user.getAsMention(), false);
                embedHighlight.setColor(0xffcd3c);

                var darwinChannel = event.getGuild()
                        .getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID);

                if (!(msgContent.length() > 1024)) {

                    embedHighlight.addField("msg", msgContent, false);
                    darwinChannel
                            .sendMessageEmbeds(embedHighlight.build())
                            .queue();
                } else {

                    embedHighlight.addField("msg", "Message was too long check attached file for content", false);

                    try {
                        var myFile = new File("long_message.txt");
                        if (myFile.createNewFile()) {

                            FileWriter fw = new FileWriter(myFile);
                            fw.write(msgContent);
                            fw.flush();
                            fw.close();

                            FileUpload.fromData(myFile);

                            darwinChannel
                                    .sendMessageEmbeds(embedHighlight.build())
                                    .queue();

                            darwinChannel.sendFiles(FileUpload.fromData(myFile)).queue();

                            myFile.delete();//ignore the warning, we want to delete this
                        }
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
           }
        }
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {

        var emoji = event.getEmoji().asUnicode();

        //TODO we could remove original highlight msgembed if it falls under the threshold->
        // ask miku if he wants this feat, if so shit would prob happen in here after we do -1 star amount

        //star emoji unicode
        if (emoji.getAsCodepoints().equalsIgnoreCase("U+2B50")) {

            var msgId = event.getMessageIdLong();
            var starMsgDao = new StarMessageDao();
            var starAmount = starMsgDao.get(msgId).orElseThrow().starAmount();
            var isSent = starMsgDao.get(msgId).orElseThrow().isSent();
            var newStarAmount = starAmount - 1;

            starMsgDao.update(new StarMessage(msgId, newStarAmount, isSent));
        }
    }
}
