package va.rembot.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.FileUpload;
import va.rembot.BotConfig;
import va.rembot.database.dao.MessageDao;
import va.rembot.database.models.DiscordMessage;
import va.rembot.exceptions.MessageNotFoundException;
import va.rembot.lib.ExtractFromMessage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class DeletedMessage extends ListenerAdapter {

    @Override
    public void onMessageDelete(MessageDeleteEvent event) {
        long messageId = event.getMessageIdLong();
        long discordUserId;
        String messageContent;
        String attachmentLinks;
        String author;
        TextChannel logChannel = event.getGuild().getChannelById(TextChannel.class, BotConfig.LOG_CHANNEL_ID);

        MessageDao messageDao = new MessageDao();
        Optional<DiscordMessage> messageObject = messageDao.get(messageId);

        discordUserId = messageObject.orElseThrow(() -> new MessageNotFoundException("Could not retrieve message from database.")).discordId();
        messageContent = messageObject.orElseThrow(() -> new MessageNotFoundException("Could not retrieve message from database.")).messageContent();
        attachmentLinks = messageObject.orElseThrow(() -> new MessageNotFoundException("Could not retrieve message from database.")).attachmentsLinks();

        Member user = event.getGuild().getMemberById(discordUserId);

        if (Objects.isNull(user)) {
            author = "Unknown author";
            messageContent = "Message content unknown. (Was not saved in database).";
        } else
            author = user.getAsMention();

        String mediaUrl = ExtractFromMessage.extractMediaUrls(messageContent);

        if (!mediaUrl.isEmpty())
            messageContent = messageContent.replace(mediaUrl, "");

        if (messageContent.length() + attachmentLinks.length() <= 1900) {
            String finalMessageContent = messageContent;
            logChannel.sendMessage("**[MESSAGE DELETED]** " + author + " <C:" + messageContent + ">\n" + attachmentLinks + mediaUrl)
                    .queue(success -> {
                        log.info("[onMessageDelete] Message was deleted by: {} message content: {} attachment links: {}", author, finalMessageContent, attachmentLinks);
                    });
        } else {
            try {
                File file = new File("long_message_deleted.txt");

                if (file.createNewFile()) {
                    FileWriter fw = new FileWriter(file);
                    fw.write(messageContent);
                    fw.flush();
                    fw.close();

                    logChannel
                            .sendMessage("**[MESSAGE DELETED]** " + author + " <C:" + "Message content was too long see attached file below for original message.>\n" + attachmentLinks + mediaUrl)
                            .and(logChannel.sendFiles(FileUpload.fromData(file)))
                            .queue();
                    file.delete();
                }

            } catch (IOException e) {
                log.error("[onMessageDelete] Something went wrong while storing deleted message as file format.");
                log.error("[onMessageDelete] Error: {}", e.getMessage());
                log.error("[onMessageDelete] Stack trace: {}", e.getStackTrace());
            }
        }
    }
}
