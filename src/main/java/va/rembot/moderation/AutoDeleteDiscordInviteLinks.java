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

        //TODO: check with miku if mods should be exempt from this if so:
//        Member member = event.getMember();
//        Role modRole = event.getJDA().getRoleById(BotConfig.getModRoleIdLong());
//
//        if (member.getUnsortedRoles().contains(modRole)) {
//            return;
//        }

        Message message = event.getMessage();
        String messageContent = message.getContentRaw();

        Pattern pattern = Pattern.compile("\\S*discord(?:\\.gg|\\.com\\/invite)\\S+");
        Matcher matcher = pattern.matcher(messageContent);

        if (matcher.find()) {
           event.getChannel()
                   .sendMessage("Discord links not allowed.")
                   .and(message.delete())
                   .queue();
        }
    }
}
