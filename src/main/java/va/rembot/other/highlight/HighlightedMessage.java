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
import va.rembot.database.models.DiscordMessage;
import va.rembot.database.models.StarMessage;
import va.rembot.database.models.DiscordUser;
import va.rembot.exceptions.MessageNotFoundException;
import va.rembot.exceptions.StarMessageNotFoundException;
import va.rembot.lib.ExtractFromMessage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Objects;
import java.util.Optional;

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

            Optional<DiscordMessage> msgObject = messageDao.get(msgId);

            if (msgObject.isEmpty()) {
                log.error("[onMessageReactionAdd] Couldn't highlight message. Message is not stored in database. This could be because the bot was started/restarted/created after the message was created. The message ID is: {}", msgId);
                return;
            }
            
            long msgAuthorId = msgObject
                    .orElseThrow(() -> new MessageNotFoundException("Failure while trying to retrieve message from database with (discord message) id: " + msgId))
                    .discordId();

            User user = event.getJDA().getUserById(msgAuthorId);
            if (Objects.isNull(user)) {
                log.error("[onMessageReactionAdd] Cannot retrieve user from database, they are not stored in the database. Cannot highlight any of their messages.");
                return;
            }

            starMessageDao.create(new StarMessage(msgId, 0, false, 0));
            Optional<StarMessage> starMsgObject = starMessageDao.get(msgId);

            int starAmount = starMsgObject
                    .orElseThrow(() -> new StarMessageNotFoundException("Failure trying to retrieve star message from database with (discord message) id: " + msgId))
                    .starAmount();

            boolean isSent = starMsgObject
                    .orElseThrow(() -> new StarMessageNotFoundException("Failure trying to retrieve star message from database with (discord message) id: " + msgId))
                    .isSent();

            long embedMsgId = starMsgObject
                    .orElseThrow(() -> new StarMessageNotFoundException("Failure trying to retrieve star message from database with (discord message) id: " + msgId))
                    .embedMessageId();

            String msgContent = msgObject
                    .orElseThrow(() -> new MessageNotFoundException("Failure while trying to retrieve message from database with (discord message) id: " + msgId))
                    .messageContent();

            String attachmentLinks = msgObject
                    .orElseThrow(() -> new MessageNotFoundException("Failure while trying to retrieve message from database with (discord message) id: " + msgId))
                    .attachmentsLinks();

            int newStarAmount = starAmount + 1;
            starMessageDao.update(new StarMessage(msgId, newStarAmount, isSent, embedMsgId));

            String mediaUrl = ExtractFromMessage.extractMediaUrls(msgContent);
            boolean hasMediaLink;

            if (mediaUrl.isEmpty())
                hasMediaLink = false;
            else
                hasMediaLink = true;

            boolean hasAttachments;
            if (attachmentLinks.isEmpty())
                hasAttachments = false;
            else
                hasAttachments = true;

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

            Optional<DiscordMessage> msgObject = messageDao.get(msgId);

            long msgAuthorId = msgObject
                    .orElseThrow(() -> new MessageNotFoundException("Failure while trying to retrieve message from database with (discord message) id: " + msgId))
                    .discordId();

            User user = event.getJDA().getUserById(msgAuthorId);
            if (Objects.isNull(user)) {
                log.error("[onMessageReactionRemove] user object is NULL, they are not stored in the database. Cannot highlight any of their messages.");
                return;
            }

            Optional<StarMessage> starMsgObject = starMessageDao.get(msgId);

            int starAmount = starMsgObject
                    .orElseThrow(() -> new StarMessageNotFoundException("Failure trying to retrieve star message from database with (discord message) id: " + msgId))
                    .starAmount();

            boolean isSent = starMsgObject
                    .orElseThrow(() -> new StarMessageNotFoundException("Failure trying to retrieve star message from database with (discord message) id: " + msgId))
                    .isSent();

            long embedMsgId = starMsgObject
                    .orElseThrow(() -> new StarMessageNotFoundException("Failure trying to retrieve star message from database with (discord message) id: " + msgId))
                    .embedMessageId();

            String msgContent = msgObject
                    .orElseThrow(() -> new MessageNotFoundException("Failure while trying to retrieve message from database with (discord message) id: " + msgId))
                    .messageContent();

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
        msgDao.create(new DiscordMessage(discordMsgId, discordId, new Timestamp(msgTimeCreated), msgContent, attachmentLinks));
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
