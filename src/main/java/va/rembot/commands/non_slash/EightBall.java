package va.rembot.commands.non_slash;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class EightBall extends ListenerAdapter {

    private static final List<String> eightBallAnswers = new ArrayList<>(Arrays
            .asList("It is certain", "It is decidedly so", "Without a doubt",
                    "Yes definitely", "You may rely on it", "As I see it, yes",
                    "Most likely", "Outlook good", "Yes", "Signs point to yes",
                    "Reply hazy, try again", "Ask again later", "Better not tell you now",
                    "Cannot predict now", "Concentrate and ask again", "Don't count on it",
                    "My reply is no", "My sources say no", "Outlook not so good", "Very doubtful"));

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        var msg = event.getMessage().getContentRaw();

        if (msg.startsWith("&8ball")){

            var rnd = new Random();
            var randomInt = rnd.nextInt(0, eightBallAnswers.toArray().length);
            var result = eightBallAnswers.get(randomInt);

            event.getMessage().reply(result).queue();
        }
    }
}
