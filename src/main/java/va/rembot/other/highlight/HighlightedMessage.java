package va.rembot.other.highlight;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
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
import va.rembot.database.models.DiscordMessage;
import va.rembot.database.models.StarMessage;
import va.rembot.database.models.DiscordUser;
import va.rembot.exceptions.MessageNotFoundException;
import va.rembot.exceptions.StarMessageNotFoundException;
import va.rembot.lib.MessageLib;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Objects;

@Slf4j
public class HighlightedMessage extends ListenerAdapter {

    private static final String STAR_EMOJI_UNICODE = "U+2B50";

    @Override
    public void onMessageReactionAdd(MessageReactionAddEvent event) {

        String emojiAsUnicode = event
                .getEmoji()
                .asUnicode()
                .getAsCodepoints();

        if (emojiAsUnicode.equalsIgnoreCase(STAR_EMOJI_UNICODE)) {

            long msgId = event.getMessageIdLong();
            StarMessageDao starMessageDao = new StarMessageDao();
            MessageDao messageDao = new MessageDao();

            DiscordMessage msgObject = messageDao
                    .get(msgId)
                    .orElseThrow(() -> new MessageNotFoundException("[onMessageReactionAdd] Failure while trying to retrieve message from database with (discord message) id: " + msgId));

            if (msgObject.discordUserId() == 0) {
                log.error("[onMessageReactionAdd] Couldn't highlight message. Message is not stored in database. This could be because the bot was started/restarted/created after the message was created. The message ID is: {}", msgId);
                return;
            }
            
            long msgAuthorId = msgObject.discordUserId();

            User user = event.getJDA().getUserById(msgAuthorId);
            if (Objects.isNull(user)) {
                log.error("[onMessageReactionAdd] Cannot retrieve user from database, they are not stored in the database. Cannot highlight any of their messages.");
                return;
            }

            starMessageDao.create(new StarMessage(msgId, 0, false, 0));
            StarMessage starMsgObject = starMessageDao.get(msgId)
                    .orElseThrow(() -> new StarMessageNotFoundException("[onMessageReactionAdd] Failure trying to retrieve star message from database with (discord message) id: " + msgId));

            int starAmount = starMsgObject.starAmount();
            boolean isSent = starMsgObject.isSent();
            long embedMsgId = starMsgObject.embedMessageId();

            String msgContent = msgObject.messageContent();
            String attachmentLinks = msgObject.attachmentsLinks();

            int newStarAmount = starAmount + 1;
            starMessageDao.update(new StarMessage(msgId, newStarAmount, isSent, embedMsgId));

            String mediaUrl = MessageLib.extractMediaUrls(msgContent);
            boolean hasMediaLink;
            hasMediaLink = !mediaUrl.isEmpty();

            boolean hasAttachments;
            hasAttachments = !attachmentLinks.isEmpty();

            if (newStarAmount >= BotConfig.getHighlightStarThresholdInt()){

                String newStarAmountAsString = Integer.valueOf(newStarAmount).toString();
                EmbedBuilder embedHighlight = getBaseEmbedMessage(user, newStarAmountAsString, msgContent);
                TextChannel darwinChannel = event.getGuild().getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID);

                if (!isSent) {

                    if (msgContent.length() <= 1024) {

                        darwinChannel
                                .sendMessageEmbeds(embedHighlight.build())
                                .queue(message -> {
                                    long newEmbedMsgId = message.getIdLong();
                                    starMessageDao.update(new StarMessage(msgId, newStarAmount, true, newEmbedMsgId));

                                    if (hasAttachments || hasMediaLink)
                                        darwinChannel.sendMessage("Attachments: " + attachmentLinks + " " + mediaUrl).queue();
                                });
                    } else {

                        try {
                            File myFile = new File("long_message.txt");
                            if (myFile.createNewFile()) {

                                FileWriter fw = new FileWriter(myFile);
                                fw.write(msgContent);
                                fw.flush();
                                fw.close();

                                darwinChannel
                                        .sendMessageEmbeds(embedHighlight.build())
                                        .queue(message -> {
                                            long newEmbedMsgId = message.getIdLong();
                                            starMessageDao.update(new StarMessage(msgId, newStarAmount, true, newEmbedMsgId));

                                            if (hasAttachments || hasMediaLink)
                                                darwinChannel.sendMessage("Attachments: " + attachmentLinks + " " + mediaUrl).queue();
                                        });

                                darwinChannel.sendFiles(FileUpload.fromData(myFile)).queue();
                                myFile.delete();
                            }
                        } catch (IOException e) {
                            log.error("[onMessageReactionAdd] Something went wrong while trying to create and upload text file to discord.");
                            log.error(e.getMessage());
                        }
                    }
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

        if (emojiAsUnicode.equalsIgnoreCase(STAR_EMOJI_UNICODE)) {

            long msgId = event.getMessageIdLong();
            StarMessageDao starMessageDao = new StarMessageDao();
            MessageDao messageDao = new MessageDao();

            DiscordMessage msgObject = messageDao
                    .get(msgId)
                    .orElseThrow(() -> new MessageNotFoundException("[onMessageReactionRemove] Failure while trying to retrieve message from database with (discord message) id: " + msgId));

            long msgAuthorId = msgObject.discordUserId();

            User user = event.getJDA().getUserById(msgAuthorId);
            if (Objects.isNull(user)) {
                log.error("[onMessageReactionRemove] user object is NULL, they are not stored in the database. Cannot highlight any of their messages.");
                return;
            }

            StarMessage starMsgObject = starMessageDao
                    .get(msgId)
                    .orElseThrow(() -> new StarMessageNotFoundException("[onMessageReactionRemove] Failure trying to retrieve star message from database with (discord message) id: " + msgId));

            int starAmount = starMsgObject.starAmount();
            boolean isSent = starMsgObject.isSent();
            long embedMsgId = starMsgObject.embedMessageId();

            String msgContent = msgObject.messageContent();

            int newStarAmount = starAmount - 1;

            String newStarAmountAsString = Integer.valueOf(newStarAmount).toString();
            EmbedBuilder embedHighlight = getBaseEmbedMessage(user, newStarAmountAsString, msgContent);
            TextChannel darwinChannel = event.getGuild().getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID);

            darwinChannel
                    .editMessageEmbedsById(embedMsgId, embedHighlight.build())
                    .queue();

            starMessageDao.update(new StarMessage(msgId, newStarAmount, isSent, embedMsgId));
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        MessageDao msgDao = new MessageDao();
        UserDao userDao = new UserDao();

        Message msg = event.getMessage();
        String msgContent = msg.getContentRaw();
        long discordMsgId = event.getMessageIdLong();
        long discordId = event.getAuthor().getIdLong();
        long msgTimeCreated = msg.getTimeCreated().toInstant().toEpochMilli();

        StringBuilder attachmentLinks = new StringBuilder();

        for (Attachment attachment : msg.getAttachments())
            attachmentLinks.append(attachment.getUrl()).append(" ");

        userDao.create(new DiscordUser(discordId));
        msgDao.create(new DiscordMessage(discordMsgId, discordId, new Timestamp(msgTimeCreated), msgContent, attachmentLinks.toString(), false, "", 0));
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
