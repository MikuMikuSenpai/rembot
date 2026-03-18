package va.rembot.moderation;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.BotConfig;

import java.util.Arrays;
import java.util.List;

public class BannedWordsFilter extends ListenerAdapter {

    private static final List<String> listBannedWords = Arrays.stream(BotConfig.BANNED_WORDS_LIST).toList();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        //TODO add a exception list for example dont ban "Niger" = country
        //TODO add a substitute method that replaces letters for their equivalent anti-censor variant
        // e.g. 1=i, @=a etc.

        var msg = event.getMessage().getContentRaw();

        if (listBannedWords.stream().anyMatch(s -> s.equals(msg))) {
            event.getMessage().getChannel().sendMessage("You said a banned word." + event.getAuthor().getAsMention()).queue();
            event.getMessage().delete().queue();
        }
    }
}
