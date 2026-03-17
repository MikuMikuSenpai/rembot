package va.rembot;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import va.rembot.commands.non_slash.PingPong;
import va.rembot.commands.slash.admin.Ban;

/// All global variables should be here
/// Configuration related to EventListeners and adding slash commands should be set in "onReady" method
/// The "onReady" method ensures rembot is fully loaded/started
@Slf4j
public class BotConfig extends ListenerAdapter {

    public static final String BOT_TOKEN = System.getenv("BOT_TOKEN");
    public static final String LOG_CHANNEL_ID = System.getenv("LOG_CHANNEL_ID");
    public static final String DARWIN_CHANNEL_ID = System.getenv("DARWIN_CHANNEL_ID");
    public static final String BANNED_WORDS = System.getenv("BANNED_WORDS");

    @Override
    public void onReady(ReadyEvent event) {

        var bot = event.getJDA();

        bot.addEventListener(
                new Ban(),
                new PingPong());

        bot.updateCommands()
                .addCommands(
                        Commands.slash("ban", "Ban someone.")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
                                .addOption(OptionType.USER, "username", "The user to be banned.", true)
                                .addOption(OptionType.STRING, "reason", "Reason for ban.", false))
                .queue(success -> log.info("Successfully loaded all slash commands."));
    }
}
