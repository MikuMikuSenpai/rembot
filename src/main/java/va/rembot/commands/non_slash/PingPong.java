package va.rembot.commands.non_slash;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.time.Duration;
import java.time.LocalDateTime;

/// example non_slash command
/// Useful for quickly checking if bot is working
public class PingPong extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        var msg = event.getMessage().getContentRaw();

        if (msg.equals("&ping")){
            var timeSent = event.getMessage().getTimeCreated().toLocalDateTime();
            var timeNow = LocalDateTime.now();
            var timeTook = Duration.between(timeSent, timeNow).abs().getNano();

            event.getMessage().reply("pong (took " + timeTook + " ns to handle)" ).queue();
        }
    }
}
