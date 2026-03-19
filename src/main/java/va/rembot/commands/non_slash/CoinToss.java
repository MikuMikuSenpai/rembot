package va.rembot.commands.non_slash;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.*;

public class CoinToss extends ListenerAdapter {

    private final List<String> headsOrTails = new ArrayList<>(Arrays.asList("heads", "tails"));

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        var msg = event.getMessage().getContentRaw();

        if (msg.equals("&cointoss")){

            Random rnd = new Random();
            var randomInt = rnd.nextInt(0, headsOrTails.toArray().length);
            var result = headsOrTails.get(randomInt);

            event.getMessage().reply(result).queue();
        }
    }
}
