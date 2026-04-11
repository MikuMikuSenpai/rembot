package va.rembot.other.highlight;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionRemoveEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import va.rembot.BotConfig;
import va.rembot.database.dao.MessageDao;
import va.rembot.database.dao.StarMessageDao;
import va.rembot.database.dao.UserDao;
import va.rembot.database.models.Message;
import va.rembot.database.models.StarMessage;
import va.rembot.database.models.User;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class HighlightedMessage extends ListenerAdapter {

    //TODO make exceptions and add them at the orelsethrow, cba atm
    //TODO there is some repeated code extract those to a method (e.g. embedbuilder)
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {

        var emoji = event.getEmoji().asUnicode();

        //only do our bs if its an actual star
        if (emoji.getAsCodepoints().equalsIgnoreCase("U+2B50")) {

            var msgId = event.getMessageIdLong();
            var starMsgDao = new StarMessageDao();
            var msgDao = new MessageDao();
            starMsgDao.create(new StarMessage(msgId, 0, false, 0));//init the bs thing

            var starAmount = 0;
            starAmount = starMsgDao.get(msgId)
                    .orElseThrow()
                    .starAmount();

            var embedMsgId = starMsgDao.get(msgId)
                    .orElseThrow()
                    .embedMessageId();

            var isSent = starMsgDao.get(msgId).orElseThrow().isSent();

            var newStarAmount = starAmount + 1;

            // update w new star amoutn
            starMsgDao.update(new StarMessage(msgId, newStarAmount, isSent, embedMsgId));

            var msgContent = msgDao.get(msgId)
                   .orElseThrow()
                   .messageContent();

            //https://stackoverflow.com/a/237068
            //i made the regex, if anyone knows how to optimize, feel free but i think this is p good,
            //we grab any URL that ends w specific file extension like .gif,... we include optional
            // query bs and fragment from the url since discord's cdn likes too spam those
            //another note i feel like case_insensitive is bandage fix but we should be fine
            // (we need this in case a file format would be written in uppercase)
            // we accept:
            //   video: MP4, MOV, MKV, AVI, WEBM
            //   picture: JPEG, PNG, GIF, HEIC, WEBP
            Pattern pattern = Pattern.compile("https?\\S+" +
                    "(\\.avi|" +
                    "\\.gif|" +
                    "\\.heic|" +
                    "\\.jpe?g|" +
                    "\\.mkv|" +
                    "\\.mov|" +
                    "\\.mp4|" +
                    "\\.png|" +
                    "\\.webm|" +
                    "\\.webp)(\\S+|)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(msgContent);
            String mediaUrl;

            boolean hasMediaLink;
            if (matcher.find()) {
               hasMediaLink = true;
               mediaUrl = matcher.group();
            } else {
                mediaUrl = "";
                hasMediaLink = false;
            }

            var msgAuthorId = msgDao.get(msgId)
                    .orElseThrow()
                    .discordId();

            var attachmentLinks = msgDao.get(msgId)
                    .orElseThrow()
                    .attachmentsLinks();

            boolean hasAttachments;
            if (!attachmentLinks.isEmpty())
                hasAttachments = true;
            else {
                hasAttachments = false;
            }

            var user = event.getJDA().getUserById(msgAuthorId);

            //notttgointolie no fucking idea why we have to check the msgcontent
            // for it to not be empty forgot how i stumbled onto this ig keep it since it doesnt hurt,
            // issent is to prevent dupes/duplicate msg highlights in darwin
            if (newStarAmount >= BotConfig.getHighlightStarThresholdInt()){

                if (!isSent) {

                    isSent = true;

                    String stars = Integer.valueOf(newStarAmount).toString();

                    EmbedBuilder embedHighlight = new EmbedBuilder();
                    embedHighlight.setTitle("⭐ message highlight ⭐");
                    embedHighlight.addField("user", user.getAsMention(), true);
                    embedHighlight.addField("stars", stars, true);
                    embedHighlight.setColor(0xffcd3c);

                    var darwinChannel = event.getGuild()
                            .getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID);

                    if (!(msgContent.length() > 1024)) {

                        if (!msgContent.isEmpty()) {
                            embedHighlight.addField("msg", msgContent, false);
                        } else {
                            embedHighlight.addField("msg", "Original msg was empty. (Check attachments below)", false);
                        }

                        boolean finalIsSent = isSent;
                        darwinChannel
                                .sendMessageEmbeds(embedHighlight.build())
                                .queue(message -> {
                                    var newEmbedMsgId = message.getIdLong();
                                    starMsgDao.update(new StarMessage(msgId, newStarAmount, finalIsSent, newEmbedMsgId));

                                    if (hasAttachments || hasMediaLink)
                                        darwinChannel.sendMessage("Attachments: " + attachmentLinks + " " + mediaUrl).queue();

                                });
                    } else {

                        embedHighlight.addField("msg", "Message was too long check attached file for content", false);

                        try {
                            var myFile = new File("long_message.txt");
                            if (myFile.createNewFile()) {

                                FileWriter fw = new FileWriter(myFile);
                                fw.write(msgContent);
                                fw.flush();
                                fw.close();

                                boolean finalIsSent1 = isSent;
                                //so for some reason using FileUpload with sendFiles
                                // makes the text file above the embed which is ugly as fk,
                                //so we do it manual way but this makes it so that
                                // if we ever want to be able to delete msghihglight + attachments
                                // we'll have to store EACH attachement msg id
                                // i think we r fine tho since wont need this
                                darwinChannel
                                        .sendMessageEmbeds(embedHighlight.build())
                                        .queue(message -> {
                                            var newEmbedMsgId = message.getIdLong();
                                            starMsgDao.update(new StarMessage(msgId, newStarAmount, finalIsSent1, newEmbedMsgId));

                                            if (hasAttachments)
                                                darwinChannel.sendMessage("Attachments: " + attachmentLinks).queue();
                                        });

                                darwinChannel.sendFiles(FileUpload.fromData(myFile)).queue();
                                myFile.delete();//ignore the warning, we want to delete this
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    //if message was already sent (we edit to update stars amount instead of sending a new embed):
                } else {

                    String stars = Integer.valueOf(newStarAmount).toString();

                    EmbedBuilder embedHighlight = new EmbedBuilder();
                    embedHighlight.setTitle("⭐ message highlight ⭐");
                    embedHighlight.addField("user", user.getAsMention(), true);
                    embedHighlight.addField("stars", stars, true);
                    embedHighlight.setColor(0xffcd3c);

                    var darwinChannel = event
                            .getGuild()
                            .getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID);

                    if (!(msgContent.length() > 1024)) {

                        if (!msgContent.isEmpty()) {
                            embedHighlight.addField("msg", msgContent, false);
                        } else {
                            embedHighlight.addField("msg", "Original msg was empty. (Check attachments below)", false);
                        }

                        darwinChannel
                                .editMessageEmbedsById(embedMsgId, embedHighlight.build())
                                .queue();
                    } else {

                        embedHighlight.addField("msg", "Message was too long check attached file for content", false);
                        darwinChannel
                                .editMessageEmbedsById(embedMsgId, embedHighlight.build())
                                .queue();
                    }
                }
           }
        }
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {

        var emoji = event.getEmoji().asUnicode();

        //star emoji unicode
        if (emoji.getAsCodepoints().equalsIgnoreCase("U+2B50")) {

            var msgId = event.getMessageIdLong();
            var starMsgDao = new StarMessageDao();
            var msgDao = new MessageDao();
            var starAmount = starMsgDao.get(msgId).orElseThrow().starAmount();
            var isSent = starMsgDao.get(msgId).orElseThrow().isSent();
            var newStarAmount = starAmount - 1;
            var msgContent = msgDao.get(msgId).orElseThrow().messageContent();

            String stars = Integer.valueOf(newStarAmount).toString();

            var msgAuthorId = msgDao.get(msgId)
                    .orElseThrow()
                    .discordId();

            var user = event.getJDA().getUserById(msgAuthorId);

            EmbedBuilder embedHighlight = new EmbedBuilder();
            embedHighlight.setTitle("⭐ message highlight ⭐");
            embedHighlight.addField("user", user.getAsMention(), true);
            embedHighlight.addField("stars", stars, true);
            embedHighlight.setColor(0xffcd3c);

            var embedMsgId = starMsgDao.get(msgId).orElseThrow().embedMessageId();

            var darwinChannel = event.getGuild().getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID);

            if (!(msgContent.length() > 1024)) {

                if (!msgContent.isEmpty()) {
                    embedHighlight.addField("msg", msgContent, false);
                } else {
                    embedHighlight.addField("msg", "Original msg was empty. (Check attachments below)", false);
                }

                darwinChannel
                        .editMessageEmbedsById(embedMsgId, embedHighlight.build())
                        .queue();
            } else {

                embedHighlight.addField("msg", "Message was too long check attached file for content", false);
                darwinChannel
                        .editMessageEmbedsById(embedMsgId, embedHighlight.build())
                        .queue();
            }

            starMsgDao.update(new StarMessage(msgId, newStarAmount, isSent, embedMsgId));
        }
    }

    @Override
    //TODO we log every message including mod's for them to be able to be msg highlighted
    // (this is normally done by other listeners atm too but redundancy is good)
    // we could do same with bots discuss w miku
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        var msgDao = new MessageDao();
        var userDao = new UserDao();

        var msg = event.getMessage();
        String msgContent = msg.getContentRaw();
        long discordMsgId = event.getMessageIdLong();
        long discordId = event.getAuthor().getIdLong();
        long msgTimeCreated = msg.getTimeCreated().toInstant().toEpochMilli();

        String messageWithLinks = "";

        for (var file : msg.getAttachments()) {
            messageWithLinks += file.getUrl() + " ";
        }

        userDao.create(new User(discordId));
        msgDao.create(new Message(discordMsgId, discordId, new Timestamp(msgTimeCreated), msgContent, messageWithLinks));
    }
}
