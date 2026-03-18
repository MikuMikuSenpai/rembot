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
import va.rembot.commands.slash.admin.Kick;
import va.rembot.commands.slash.admin.Mute;
import va.rembot.commands.slash.admin.Unban;
import va.rembot.moderation.BannedWordsFilter;

/// All global variables should be here
/// Configuration related to EventListeners and adding slash commands should be set in "onReady" method
/// The "onReady" method ensures rembot is fully loaded/started
@Slf4j
public class BotConfig extends ListenerAdapter {

    public static final String BOT_TOKEN = System.getenv("BOT_TOKEN");
    public static final String LOG_CHANNEL_ID = System.getenv("LOG_CHANNEL_ID");
    public static final String DARWIN_CHANNEL_ID = System.getenv("DARWIN_CHANNEL_ID");
    public static final String BANNED_WORDS = System.getenv("BANNED_WORDS");
    public static final String[] BANNED_WORDS_LIST = BANNED_WORDS.split(",");

    @Override
    public void onReady(ReadyEvent event) {

        var bot = event.getJDA();

        bot.addEventListener(
                new Ban(),
                new Unban(),
                new Kick(),
                new Mute(),
                new BannedWordsFilter(),
                new PingPong());

        bot.updateCommands()
                .addCommands(
                        Commands.slash("ban", "Ban someone.")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
                                .addOption(OptionType.USER, "username", "The user to be banned.", true)
                                .addOption(OptionType.STRING, "reason", "Reason for ban.", false),
                        Commands.slash("unban", "Unban someone.")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.BAN_MEMBERS))
                                .addOption(OptionType.USER, "username", "The user to be unbanned.", true)
                                .addOption(OptionType.STRING, "reason", "Reason for unban.", false),
                        Commands.slash("kick", "Kick someone.")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.KICK_MEMBERS))
                                .addOption(OptionType.USER, "username", "The user to be kicked.", true)
                                .addOption(OptionType.STRING, "reason", "Reason for kick.", false),
                        Commands.slash("mute", "Mute someone.")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS))
                                .addOption(OptionType.USER, "username", "The user to be muted.", true)
                                .addOption(OptionType.INTEGER, "minutes", "The amount of minutes to be muted for.", true)
                                .addOption(OptionType.STRING, "reason", "Reason for mute.", false)
                                .addOption(OptionType.INTEGER, "hours", "The amount of hours to be muted for.", false))
                .queue(success -> log.info("Successfully loaded all slash commands."));
    }
}
