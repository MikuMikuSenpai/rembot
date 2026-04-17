package va.rembot.other.highlight;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
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
import va.rembot.database.models.DiscordUser;
import va.rembot.exceptions.MessageNotFoundException;
import va.rembot.exceptions.StarMessageNotFoundException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class HighlightedMessage extends ListenerAdapter {

    private static String starEmojiUnicode = "U+2B50";

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

            var msgObject = msgDao.get(msgId);

            long msgAuthorId = msgObject
                    .orElseThrow(() -> new MessageNotFoundException("Failure while trying to retrieve message from database with id: ", msgId))
                    .discordId();

            //this currently returns null if used on bots since we dont store their ID in DB,
            // this is currently our expected behavior if we want to support bots
            // we'd just have to store their messages in DB
            // currently cus msg doesnt exist we return a default Message object with defaults: userid as 0 which
            // obv doesnt exist so results in null below
            var user = event.getJDA().getUserById(msgAuthorId);
            if (Objects.isNull(user)) {
                log.error("[onMessageReactionAdd] user object is NULL, they are not stored in the database. Cannot highlight any of their messages.");
                return;
            }

            starMsgDao.create(new StarMessage(msgId, 0, false, 0));//init the bs thing

            // we query this one later as perf improvement in case the user is null above
            var starMsgObject = starMsgDao.get(msgId);

            int starAmount = starMsgObject
                    .orElseThrow(() -> new StarMessageNotFoundException("Failure trying to retrieve star message from database with id: ", msgId))
                    .starAmount();

            boolean isSent = starMsgObject
                    .orElseThrow(() -> new StarMessageNotFoundException("Failure trying to retrieve star message from database with id: ", msgId))
                    .isSent();

            long embedMsgId = starMsgObject
                    .orElseThrow(() -> new StarMessageNotFoundException("Failure trying to retrieve star message from database with id: ", msgId))
                    .embedMessageId();

            String msgContent = msgObject
                    .orElseThrow(() -> new MessageNotFoundException("Failure while trying to retrieve message from database with id: ", msgId))
                    .messageContent();

            String attachmentLinks = msgObject
                    .orElseThrow(() -> new MessageNotFoundException("Failure while trying to retrieve message from database with id: ", msgId))
                    .attachmentsLinks();

            int newStarAmount = starAmount + 1;

            // update w new star amount
            starMsgDao.update(new StarMessage(msgId, newStarAmount, isSent, embedMsgId));

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
                hasMediaLink = false;
                mediaUrl = "";
            }

            boolean hasAttachments;
            if (attachmentLinks.isEmpty())
                hasAttachments = false;
            else {
                hasAttachments = true;
            }

            if (newStarAmount >= BotConfig.getHighlightStarThresholdInt()){

                String stars = Integer.valueOf(newStarAmount).toString();
                EmbedBuilder embedHighlight = getBaseEmbedMessage(user, stars, msgContent);
                TextChannel darwinChannel = event.getGuild()
                        .getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID);

                //prevent sending duplicates
                if (!isSent) {

                    isSent = true;

                    if (msgContent.length() <= 1024) {

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

                                boolean finalIsSent = isSent;
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
                                            starMsgDao.update(new StarMessage(msgId, newStarAmount, finalIsSent, newEmbedMsgId));

                                            if (hasAttachments)
                                                darwinChannel.sendMessage("Attachments: " + attachmentLinks).queue();
                                        });

                                darwinChannel.sendFiles(FileUpload.fromData(myFile)).queue();
                                myFile.delete();//ignore the warning, we want to delete this
                            }
                        } catch (IOException e) {
                            log.error("[onMessageReactionAdd] Something went wrong while trying to create and upload text file to discord.");
                            log.error(e.getMessage());
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

            var msgObject = msgDao.get(msgId);

            long msgAuthorId = msgObject
                    .orElseThrow(() -> new MessageNotFoundException("Failure while trying to retrieve message from database with id: ", msgId))
                    .discordId();

            User user = event.getJDA().getUserById(msgAuthorId);
            if (Objects.isNull(user)) {
                log.error("[onMessageReactionRemove] user object is NULL, they are not stored in the database. Cannot highlight any of their messages.");
                return;
            }

            var starMsgObject = starMsgDao.get(msgId);

            int starAmount = starMsgObject
                    .orElseThrow(() -> new StarMessageNotFoundException("Failure trying to retrieve star message from database with id: ", msgId))
                    .starAmount();

            boolean isSent = starMsgObject
                    .orElseThrow(() -> new StarMessageNotFoundException("Failure trying to retrieve star message from database with id: ", msgId))
                    .isSent();

            long embedMsgId = starMsgObject
                    .orElseThrow(() -> new StarMessageNotFoundException("Failure trying to retrieve star message from database with id: ", msgId))
                    .embedMessageId();

            String msgContent = msgObject
                    .orElseThrow(() -> new MessageNotFoundException("Failure while trying to retrieve message from database with id: ", msgId))
                    .messageContent();

            int newStarAmount = starAmount - 1;

            String stars = Integer.valueOf(newStarAmount).toString();

            EmbedBuilder embedHighlight = getBaseEmbedMessage(user, stars, msgContent);

            TextChannel darwinChannel = event.getGuild().getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID);

            darwinChannel
                    .editMessageEmbedsById(embedMsgId, embedHighlight.build())
                    .queue();

            starMsgDao.update(new StarMessage(msgId, newStarAmount, isSent, embedMsgId));
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        var msgDao = new MessageDao();
        var userDao = new UserDao();

        var msg = event.getMessage();
        String msgContent = msg.getContentRaw();
        long discordMsgId = event.getMessageIdLong();
        long discordId = event.getAuthor().getIdLong();
        long msgTimeCreated = msg.getTimeCreated().toInstant().toEpochMilli();

        String attachmentLinks = "";

        for (var file : msg.getAttachments()) {
            attachmentLinks += file.getUrl() + " ";
        }

        userDao.create(new DiscordUser(discordId));
        msgDao.create(new Message(discordMsgId, discordId, new Timestamp(msgTimeCreated), msgContent, attachmentLinks));
    }

    private static EmbedBuilder getBaseEmbedMessage(User user, String stars, String messageContent) {

        EmbedBuilder embedHighlight = new EmbedBuilder();
        embedHighlight.setTitle("⭐ message highlight ⭐");
        embedHighlight.addField("user", user.getAsMention(), true);
        embedHighlight.addField("stars", stars, true);
        embedHighlight.setColor(0xffcd3c);

        if (messageContent.length() <= 1024) {

            if (messageContent.isEmpty()) {
                embedHighlight.addField("msg", "Original msg was empty. (Check attachments below)", false);
            } else {
                embedHighlight.addField("msg", messageContent, false);
            }

        } else {
            embedHighlight.addField("msg", "Message was too long check attached file for content", false);
        }

        return embedHighlight;
    }
}
