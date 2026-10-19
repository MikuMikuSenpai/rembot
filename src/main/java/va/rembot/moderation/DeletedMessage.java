package va.rembot.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.BotConfig;
import va.rembot.database.dao.MessageDao;
import va.rembot.database.models.DiscordMessage;
import va.rembot.exceptions.MessageNotFoundException;
import va.rembot.lib.GenericLib;
import va.rembot.lib.MessageLib;

import java.util.Objects;

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
        DiscordMessage messageObject = messageDao.get(messageId)
                .orElseThrow(() -> new MessageNotFoundException("[onMessageDelete] Could not retrieve discord message from DB."));

        discordUserId = messageObject.discordUserId();
        messageContent = messageObject.messageContent();
        attachmentLinks = messageObject.attachmentsLinks();

        Member user = event.getGuild().getMemberById(discordUserId);

        if (Objects.isNull(user)) {
            author = "Unknown author";
            messageContent = "Message content unknown. (Was not saved in database).";
        } else
            author = user.getAsMention();

        String mediaUrl = MessageLib.extractMediaUrls(messageContent);
        String[] mediaUrlAsArray = mediaUrl.split(" ");

        if (!mediaUrl.isEmpty()) {
            for (String mediaUrlEntry : mediaUrlAsArray) {
                messageContent = messageContent.replace(mediaUrlEntry, "");
            }
        }

        if (MessageLib.endsWithUrl(messageContent)) {
            messageContent = messageContent + " ";
        } else {
            messageContent = messageContent.trim();
        }

        if (messageContent.length() + attachmentLinks.length() + mediaUrl.length() <= 1900) {
            logChannel.sendMessage("**[MESSAGE DELETED]** " + author + " <C:" + messageContent + ">\n" + attachmentLinks + mediaUrl)
                    .queue(success -> {
                        log.info("[onMessageDelete] Message was deleted: {}", messageObject);
                    });
        } else {
            String finalMessageContent = messageContent;
            logChannel
                    .sendMessage("**[MESSAGE DELETED]** " + author + " <C:" + "Message content was too long see attached file below for original message.>\n" + attachmentLinks + mediaUrl)
                    .queue(success -> {
                        GenericLib.sendTextAsAttachedFile(
                                event,
                                finalMessageContent,
                                "long_message_deleted.txt",
                                BotConfig.LOG_CHANNEL_ID);
                        log.debug("[onMessageDelete] Message was deleted by: {} message content: {} attachment links: {}", user, finalMessageContent, attachmentLinks);
                    });
        }
    }
}
