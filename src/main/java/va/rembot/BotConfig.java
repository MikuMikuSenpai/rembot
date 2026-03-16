package va.rembot;

import lombok.Getter;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

/// All global variables should be here
/// Configuration related to EventListeners and adding slash commands should be set in "onReady" method
/// The "onReady" method ensures rembot is fully loaded/started
public class BotConfig extends ListenerAdapter {

    @Getter
    private static final String BOT_TOKEN = System.getenv("BOT_TOKEN");
    private static final String LOG_CHANNEL_ID = System.getenv("LOG_CHANNEL_ID");
    private static final String DARWIN_CHANNEL_ID = System.getenv("DARWIN_CHANNEL_ID");
    private static final String BANNED_WORDS = System.getenv("BANNED_WORDS");

    @Override
    public void onReady(ReadyEvent event) {

        var bot = event.getJDA();

        //TODO add the event listeners
        bot.addEventListener();

        //TODO add the slash commands
        bot.updateCommands().addCommands().queue();

    }
}
