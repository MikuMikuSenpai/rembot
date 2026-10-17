package va.rembot.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import va.rembot.BotConfig;
import va.rembot.database.dao.MessageDao;
import va.rembot.database.models.DiscordMessage;
import va.rembot.lib.MessageLib;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;

@Slf4j
public class EditedMessage extends ListenerAdapter {

    @Override
    public void onMessageUpdate(MessageUpdateEvent event) {
        if (event.getAuthor().isBot()) return;

        TextChannel logChannel = event.getGuild().getChannelById(TextChannel.class, BotConfig.LOG_CHANNEL_ID);
        long messageId = event.getMessageIdLong();

        MessageDao messageDao = new MessageDao();
        DiscordMessage message = messageDao.get(messageId).orElseThrow(() -> new RuntimeException("[onMessageUpdate] Could not get message from DB."));

        DiscordMessage messageCopy;
        if (message.discordUserId() == 0) {
            long discordUserId = event.getAuthor().getIdLong();
            long createdEpochMilli = event.getMessage().getTimeCreated().toInstant().toEpochMilli();
            String placeholder = "UNKNOWN (message was not stored in DB)";

            messageDao.create(new DiscordMessage(
                    messageId,
                    discordUserId,
                    new Timestamp(createdEpochMilli),
                    placeholder,
                    "",
                    false,
                    "",
                    0));
            messageCopy = messageDao.get(messageId).orElseThrow(() -> new RuntimeException("[onMessageUpdate] Could not get message from DB."));
            message = messageCopy;
        }

        String messageContentFromDB = message.messageContent();
        String messageContentFromEvent = event.getMessage().getContentRaw();

        log.debug("messageContentFromDB: {}", messageContentFromDB);
        log.debug("messageContentFromEvent: {}", messageContentFromEvent);

        if (messageContentFromEvent.equals(messageContentFromDB)) return;
        /*
            immediately update DB, if message has a link that can be embed it would trigger another MessageUpdateEvent
            This would cause a duplicate log message which we don't want. Updating the message_content let's us
            textually compare (message_content in DB) and prevent duped logs
            see: https://github.com/discord/discord-api-docs/issues/6500
         */
        messageDao.update(new DiscordMessage(
                message.discordMessageId(),
                message.discordUserId(),
                message.timeCreated(),
                messageContentFromEvent,
                message.attachmentsLinks(),
                true,
                "",
                message.editedLogCounter()));

        String messageAuthor = event.getAuthor().getAsMention();
        boolean isEdited = message.isEdited();
        String jumpUrl = message.editedLogJumpUrl();
        String eventMediaUrl = MessageLib.extractMediaUrls(messageContentFromEvent);
        String[] eventMediaUrlAsArray = eventMediaUrl.split(" ");

        String messageContentFromEventCopyPreStringManipul = messageContentFromEvent;
        String messageContentFromDBCopyPreStringManipul = messageContentFromDB;

        if (!eventMediaUrl.isEmpty()) {
            for (String eventMediaUrlEntry : eventMediaUrlAsArray) {
                messageContentFromEvent = messageContentFromEvent.replace(eventMediaUrlEntry, "");
            }
        }

        if (MessageLib.endsWithUrl(messageContentFromEvent)) {
            messageContentFromEvent = messageContentFromEvent + " ";
        } else {
            messageContentFromEvent = messageContentFromEvent.trim();
        }

        String mediaUrl = MessageLib.extractMediaUrls(messageContentFromDB);
        String[] mediaUrlAsArray = mediaUrl.split(" ");

        if (!mediaUrl.isEmpty()) {
            for (String mediaUrlEntry : mediaUrlAsArray) {
                messageContentFromDB = messageContentFromDB.replace(mediaUrlEntry, "");
            }
        }

        if (MessageLib.endsWithUrl(messageContentFromDB)) {
            messageContentFromDB = messageContentFromDB + " ";
        } else {
            messageContentFromDB = messageContentFromDB.trim();
        }

        String messageContentFromEventCopy = messageContentFromEvent;
        int originalMessageLength = messageContentFromDB.length() + message.attachmentsLinks().length() + mediaUrl.length();
        int eventMessageLength = messageContentFromEvent.length() + eventMediaUrl.length();

        // https://discord.com/channels/server_id/channel_id/message_id
        String linkToMessage = "https://discord.com/channels/" + event.getGuild().getIdLong() + "/" + event.getGuildChannel().getIdLong() + "/" + messageId;
        final DiscordMessage finalMessage = message;

        // kinda spagheti but fk it we roll with this
        if (!isEdited) {
            if (originalMessageLength < 1900) {
                logChannel.sendMessage("**[MESSAGE EDITED]** " + linkToMessage + " " + messageAuthor + " <O:" + messageContentFromDB + ">\n" + message.attachmentsLinks() + mediaUrl)
                        .queue(originalMessage -> {

                            if (eventMessageLength < 1900) {
                                //original = short | edited = short
                                logChannel.sendMessage("**[MESSAGE EDITED]** " + messageAuthor + " <E1:" + messageContentFromEventCopy + ">\n" + eventMediaUrl)
                                    .queue(editedMessage -> {
                                        logChannel.sendMessage(originalMessage.getJumpUrl()).queue();
                                        messageDao.update(new DiscordMessage(
                                                finalMessage.discordMessageId(),
                                                finalMessage.discordUserId(),
                                                finalMessage.timeCreated(),
                                                messageContentFromEventCopyPreStringManipul,
                                                finalMessage.attachmentsLinks(),
                                                true,
                                                editedMessage.getJumpUrl(),
                                                finalMessage.editedLogCounter() + 2));
                                    });
                            } else {

                                //original = short | edited = long
                                logChannel.sendMessage("**[MESSAGE EDITED]** " + messageAuthor + " <E1:message too long. Check file below." + ">\n" + eventMediaUrl)
                                        .queue(editedMessage -> {
                                            try {
                                                File myFile = new File("edited_content_too_long.txt");
                                                if (myFile.createNewFile()) {
                                                    FileWriter fw = new FileWriter(myFile);
                                                    fw.write(messageContentFromEventCopyPreStringManipul);
                                                    fw.flush();
                                                    fw.close();
                                                    logChannel.sendFiles(FileUpload.fromData(myFile)).queue();
                                                    myFile.delete();
                                                }
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }

                                            logChannel.sendMessage(originalMessage.getJumpUrl()).queue();
                                            messageDao.update(new DiscordMessage(
                                                    finalMessage.discordMessageId(),
                                                    finalMessage.discordUserId(),
                                                    finalMessage.timeCreated(),
                                                    messageContentFromEventCopyPreStringManipul,
                                                    finalMessage.attachmentsLinks(),
                                                    true,
                                                    editedMessage.getJumpUrl(),
                                                    finalMessage.editedLogCounter() + 2));
                                        });
                            }
                        });
            } else {
                logChannel.sendMessage("**[MESSAGE EDITED]** " + linkToMessage + " " + messageAuthor + " <O:message too long. Check file below." + ">\n" + message.attachmentsLinks() + mediaUrl)
                        .queue(originalMessage -> {

                            File file = new File("orginal_content_too_long.txt");
                            try {
                                if (file.createNewFile()) {
                                    FileWriter fw = new FileWriter(file);
                                    fw.write(messageContentFromDBCopyPreStringManipul);
                                    fw.flush();
                                    fw.close();
                                    logChannel.sendFiles(FileUpload.fromData(file)).queue();
                                    file.delete();
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }

                            if (eventMessageLength < 1900) {
                                //original = long | edited = short
                                logChannel.sendMessage("**[MESSAGE EDITED]** " + messageAuthor + " <E1:" + messageContentFromEventCopy + ">\n" + eventMediaUrl)
                                        .queue(editedMessage -> {
                                            logChannel.sendMessage(originalMessage.getJumpUrl()).queue();
                                            messageDao.update(new DiscordMessage(
                                                    finalMessage.discordMessageId(),
                                                    finalMessage.discordUserId(),
                                                    finalMessage.timeCreated(),
                                                    messageContentFromEventCopyPreStringManipul,
                                                    finalMessage.attachmentsLinks(),
                                                    true,
                                                    editedMessage.getJumpUrl(),
                                                    finalMessage.editedLogCounter() + 2));
                                        });
                            } else {
                                //original = long | edited = long
                                logChannel.sendMessage("**[MESSAGE EDITED]** " + messageAuthor + " <E1:message too long. Check file below." + ">\n" + eventMediaUrl)
                                        .queue(editedMessage -> {
                                            try {
                                                File myFile = new File("edited_content_too_long.txt");
                                                if (myFile.createNewFile()) {
                                                    FileWriter fw = new FileWriter(myFile);
                                                    fw.write(messageContentFromEventCopyPreStringManipul);
                                                    fw.flush();
                                                    fw.close();
                                                    logChannel.sendFiles(FileUpload.fromData(myFile)).queue();
                                                    myFile.delete();
                                                }
                                            } catch (IOException e) {
                                                throw new RuntimeException(e);
                                            }

                                            logChannel.sendMessage(originalMessage.getJumpUrl()).queue();
                                            messageDao.update(new DiscordMessage(
                                                    finalMessage.discordMessageId(),
                                                    finalMessage.discordUserId(),
                                                    finalMessage.timeCreated(),
                                                    messageContentFromEventCopyPreStringManipul,
                                                    finalMessage.attachmentsLinks(),
                                                    true,
                                                    editedMessage.getJumpUrl(),
                                                    finalMessage.editedLogCounter() + 2));
                                        });
                            }
                        });
            }
        } else {
            if (eventMessageLength < 1900) {
                logChannel.sendMessage("**[MESSAGE EDITED]** " + messageAuthor + " <E" + finalMessage.editedLogCounter() + ":" + messageContentFromEventCopy + ">\n" + eventMediaUrl)
                        .queue(editedMessage -> {
                            logChannel.sendMessage(jumpUrl).queue();
                            messageDao.update(new DiscordMessage(
                                    finalMessage.discordMessageId(),
                                    finalMessage.discordUserId(),
                                    finalMessage.timeCreated(),
                                    messageContentFromEventCopyPreStringManipul,
                                    finalMessage.attachmentsLinks(),
                                    true,
                                    editedMessage.getJumpUrl(),
                                    finalMessage.editedLogCounter() + 1));
                        });

            } else {
                logChannel.sendMessage("**[MESSAGE EDITED]** " + messageAuthor + " <E" + finalMessage.editedLogCounter() + ":message too long. Check file below." + ">\n" + eventMediaUrl)
                        .queue(editedMessage -> {
                            try {
                                File myFile = new File("edited_content_too_long.txt");
                                if (myFile.createNewFile()) {
                                    FileWriter fw = new FileWriter(myFile);
                                    fw.write(messageContentFromEventCopy);
                                    fw.flush();
                                    fw.close();
                                    logChannel.sendFiles(FileUpload.fromData(myFile)).queue();
                                    myFile.delete();
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            logChannel.sendMessage(jumpUrl).queue();
                            messageDao.update(new DiscordMessage(
                                    finalMessage.discordMessageId(),
                                    finalMessage.discordUserId(),
                                    finalMessage.timeCreated(),
                                    messageContentFromEventCopy,
                                    finalMessage.attachmentsLinks(),
                                    true,
                                    editedMessage.getJumpUrl(),
                                    finalMessage.editedLogCounter() + 1));
                        });
            }
        }
    }
}
