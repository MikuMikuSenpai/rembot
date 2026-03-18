package va.rembot.commands.non_slash;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.text.DecimalFormat;
import java.time.Duration;
import java.time.LocalDateTime;

/// example non_slash command
/// Useful for quickly checking if bot is working
public class PingPong extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        var msg = event.getMessage();
        var msgRaw = msg.getContentRaw();

        if (msgRaw.equals("&ping")){
            var timeSent = msg.getTimeCreated().toLocalDateTime();
            var timeNow = LocalDateTime.now();
            var timeTook = Duration.between(timeSent, timeNow).abs().getNano();
            var timeTookInMs = (double) timeTook / 1_000_000;
            var df = new DecimalFormat("#.##"); // only print 2 decimals

            event.getMessage().reply("pong (took " + df.format(timeTookInMs) + " ms)" ).queue();
        }
    }
}
