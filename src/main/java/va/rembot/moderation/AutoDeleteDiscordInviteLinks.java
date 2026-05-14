package va.rembot.moderation;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoDeleteDiscordInviteLinks extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        Message message = event.getMessage();
        String messageContent = message.getContentRaw();

        Pattern pattern = Pattern.compile("\\S*discord(?:\\.gg|\\.com\\/invite)\\S+");
        Matcher matcher = pattern.matcher(messageContent);

        if (matcher.find()) {
           event.getChannel()
                   .sendMessage("Discord invite links are not allowed.")
                   .and(message.delete())
                   .queue();
        }
    }
}
