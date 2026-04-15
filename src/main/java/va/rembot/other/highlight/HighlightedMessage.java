package va.rembot.other.highlight;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.GenericMessageReactionEvent;
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

    private static String starEmojiUnicode = "U+2B50";

    //TODO make exceptions and add them at the orelsethrow, cba atm
    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {

        String emojiAsUnicode = event
                .getEmoji()
                .asUnicode()
                .getAsCodepoints();

        if (emojiAsUnicode.equalsIgnoreCase(starEmojiUnicode)) {

            long msgId = event.getMessageIdLong();
            var starMsgDao = new StarMessageDao();
            var msgDao = new MessageDao();
            starMsgDao.create(new StarMessage(msgId, 0, false, 0));//init the bs thing

            int starAmount = starMsgDao.get(msgId)
                    .orElseThrow()
                    .starAmount();

            long embedMsgId = starMsgDao.get(msgId)
                    .orElseThrow()
                    .embedMessageId();

            boolean isSent = starMsgDao.get(msgId)
                    .orElseThrow()
                    .isSent();

            int newStarAmount = starAmount + 1;

            // update w new star amount
            starMsgDao.update(new StarMessage(msgId, newStarAmount, isSent, embedMsgId));

            String msgContent = msgDao.get(msgId)
                   .orElseThrow()
                   .messageContent();

            //extract media URL from message
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

            long msgAuthorId = msgDao.get(msgId)
                    .orElseThrow()
                    .discordId();

            String attachmentLinks = msgDao.get(msgId)
                    .orElseThrow()
                    .attachmentsLinks();

            boolean hasAttachments;
            if (!attachmentLinks.isEmpty())
                hasAttachments = true;
            else {
                hasAttachments = false;
            }

            var user = event.getJDA().getUserById(msgAuthorId);

            if (newStarAmount >= BotConfig.getHighlightStarThresholdInt()){

                String stars = Integer.valueOf(newStarAmount).toString();
                EmbedBuilder embedHighlight =  getBaseEmbedMessage(user, stars, msgContent, event);
                TextChannel darwinChannel = event.getGuild()
                        .getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID);

                //prevent sending duplicates
                if (!isSent) {

                    isSent = true;

                    if (!(msgContent.length() > 1024)) {

                        boolean finalIsSent = isSent;
                        darwinChannel
                                .sendMessageEmbeds(embedHighlight.build())
                                .queue(message -> {
                                    long newEmbedMsgId = message.getIdLong();
                                    starMsgDao.update(new StarMessage(msgId, newStarAmount, finalIsSent, newEmbedMsgId));

                                    if (hasAttachments || hasMediaLink)
                                        darwinChannel.sendMessage("Attachments: " + attachmentLinks + " " + mediaUrl).queue();

                                });
                    } else {

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

                    darwinChannel
                            .editMessageEmbedsById(embedMsgId, embedHighlight.build())
                            .queue();
                }
           }
        }
    }

    @Override
    public void onMessageReactionRemove(MessageReactionRemoveEvent event) {

        String emojiAsUnicode = event
                .getEmoji()
                .asUnicode()
                .getAsCodepoints();

        if (emojiAsUnicode.equalsIgnoreCase(starEmojiUnicode)) {

            long msgId = event.getMessageIdLong();
            var starMsgDao = new StarMessageDao();
            var msgDao = new MessageDao();
            int starAmount = starMsgDao.get(msgId)
                    .orElseThrow()
                    .starAmount();
            boolean isSent = starMsgDao.get(msgId)
                    .orElseThrow()
                    .isSent();
            int newStarAmount = starAmount - 1;
            String msgContent = msgDao.get(msgId)
                    .orElseThrow()
                    .messageContent();

            String stars = Integer.valueOf(newStarAmount).toString();

            long msgAuthorId = msgDao.get(msgId)
                    .orElseThrow()
                    .discordId();

            var user = event.getJDA().getUserById(msgAuthorId);

            EmbedBuilder embedHighlight = getBaseEmbedMessage(user, stars, msgContent, event);

            long embedMsgId = starMsgDao
                    .get(msgId)
                    .orElseThrow()
                    .embedMessageId();
            TextChannel darwinChannel = event.getGuild().getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID);

            darwinChannel
                    .editMessageEmbedsById(embedMsgId, embedHighlight.build())
                    .queue();

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

    //TODO fix name collision change our model's name User to something else;
    // clashes with jda's User class
    private static EmbedBuilder getBaseEmbedMessage(net.dv8tion.jda.api.entities.User user, String stars, String messageContent, GenericMessageReactionEvent event) {

        //TODO check that user isnt NULL, e.g. since we dont store bots atm at least their ID isnt in our DB,
        // when we call the bulshit below itll throw nullpointer exception,
        // rembot doesnt crash but the error is not being handled, host could get confused if they spot the error in terminal
        EmbedBuilder embedHighlight = new EmbedBuilder();
        embedHighlight.setTitle("⭐ message highlight ⭐");
        embedHighlight.addField("user", user.getAsMention(), true);
        embedHighlight.addField("stars", stars, true);
        embedHighlight.setColor(0xffcd3c);

        if (!(messageContent.length() > 1024)) {

            if (!messageContent.isEmpty()) {
                embedHighlight.addField("msg", messageContent, false);
            } else {
                embedHighlight.addField("msg", "Original msg was empty. (Check attachments below)", false);
            }

        } else {
            embedHighlight.addField("msg", "Message was too long check attached file for content", false);
        }

        return embedHighlight;
    }
}
