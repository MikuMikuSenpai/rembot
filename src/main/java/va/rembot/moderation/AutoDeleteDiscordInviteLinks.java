package va.rembot.moderation;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.lib.MessageLib;

public class AutoDeleteDiscordInviteLinks extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        Message message = event.getMessage();
        String messageContent = message.getContentRaw();

        boolean messageIncludesDiscordInviteLink = MessageLib.hasDiscordInviteLink(messageContent);

        if (messageIncludesDiscordInviteLink) {
            event.getChannel()
                    .sendMessage("Discord invite links are not allowed.")
                    .and(message.delete())
                    .queue();
        }
    }
}
