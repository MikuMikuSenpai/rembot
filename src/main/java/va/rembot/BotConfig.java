package va.rembot;

import lombok.Getter;
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
import va.rembot.moderation.AntiSpamFilter;
import va.rembot.moderation.word_filter.BannedWordsFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// All global variables should be here
/// Configuration related to EventListeners and adding slash commands should be set in "onReady" method
/// The "onReady" method ensures rembot is fully loaded/started
@Slf4j
public class BotConfig extends ListenerAdapter {

    public static final String BOT_TOKEN = System.getenv("BOT_TOKEN");
    public static final String MOD_ROLE_ID = System.getenv("MOD_ROLE_ID");
    @Getter
    private static Long modRoleIdLong = 0L;
    public static final String LOG_CHANNEL_ID = System.getenv("LOG_CHANNEL_ID");
    public static final String DARWIN_CHANNEL_ID = System.getenv("DARWIN_CHANNEL_ID");
    public static final String BANNED_WORDS = System.getenv("BANNED_WORDS");
    public static final String[] BANNED_WORDS_ARRAY_TEMP = BANNED_WORDS.split(",");
    public static final String[] BANNED_WORDS_ARRAY = getBannedWordsArray();
    public static final String WHITELISTED_WORDS = System.getenv("WHITELISTED_WORDS");
    public static final String[] WHITELISTED_WORDS_ARRAY = WHITELISTED_WORDS.split(",");
    public static final String MYSQL_ROOT_PASSWORD = System.getenv("MYSQL_ROOT_PASSWORD");
    public static final String MYSQL_DATABASE = System.getenv("MYSQL_DATABASE");
    public static final String ANTI_SPAM_WORDS_AMOUNT = System.getenv("ANTI_SPAM_WORDS_AMOUNT");
    public static final String ANTI_SPAM_TIME_AMOUNT = System.getenv("ANTI_SPAM_TIME_AMOUNT");
    @Getter
    private static int antiSpamTimeAmountInt = 0;
    public static final String ANTI_SPAM_MUTE_AMOUNT = System.getenv("ANTI_SPAM_MUTE_AMOUNT");
    @Getter
    private static int antiSpamMuteAmountInt = 0;
    public static final String ANTI_SPAM_STRIKE_AMOUNT = System.getenv("ANTI_SPAM_STRIKE_AMOUNT");
    @Getter
    private static int antiSpamStrikeAmountInt = 0;

    @Override
    public void onReady(ReadyEvent event) {

        var bot = event.getJDA();

        try {
            modRoleIdLong = Long.parseLong(MOD_ROLE_ID);
        } catch (NumberFormatException e) {

            log.error("[onReady] MOD_ROLE_ID ENV VAR MISSING Check your .env file it is missing values use .env.example as a guide.");
            log.error("[onReady] MOD_ROLE_ID ENV VAR MISSING The bot cannot start until this is fixed.");
            log.error("[onReady] MOD_ROLE_ID ENV VAR MISSING error: {}", e.getMessage());
            bot.shutdown();

        }

        try {
            antiSpamTimeAmountInt = Integer.parseInt(ANTI_SPAM_TIME_AMOUNT);
        } catch (NumberFormatException e) {

            log.error("[onReady] ANTI_SPAM_TIME_AMOUNT ENV VAR MISSING Check your .env file it is missing values use .env.example as a guide.");
            log.error("[onReady] ANTI_SPAM_TIME_AMOUNT ENV VAR MISSING The bot cannot start until this is fixed.");
            log.error("[onReady] ANTI_SPAM_TIME_AMOUNT ENV VAR MISSING error: {}", e.getMessage());
            bot.shutdown();

        }

        try {
            antiSpamMuteAmountInt = Integer.parseInt(ANTI_SPAM_MUTE_AMOUNT);
        } catch (NumberFormatException e) {

            log.error("[onReady] ANTI_SPAM_MUTE_AMOUNT ENV VAR MISSING Check your .env file it is missing values use .env.example as a guide.");
            log.error("[onReady] ANTI_SPAM_MUTE_AMOUNT ENV VAR MISSING The bot cannot start until this is fixed.");
            log.error("[onReady] ANTI_SPAM_MUTE_AMOUNT ENV VAR MISSING error: {}", e.getMessage());
            bot.shutdown();

        }

        try {
            antiSpamStrikeAmountInt = Integer.parseInt(ANTI_SPAM_STRIKE_AMOUNT);
        } catch (NumberFormatException e) {

            log.error("[onReady] ANTI_SPAM_STRIKE_AMOUNT ENV VAR MISSING Check your .env file it is missing values use .env.example as a guide.");
            log.error("[onReady] ANTI_SPAM_STRIKE_AMOUNT ENV VAR MISSING The bot cannot start until this is fixed.");
            log.error("[onReady] ANTI_SPAM_STRIKE_AMOUNT ENV VAR MISSING error: {}", e.getMessage());
            bot.shutdown();

        }

        bot.addEventListener(
                new Ban(),
                new Unban(),
                new Kick(),
                new Mute(),
                new BannedWordsFilter(),
                new AntiSpamFilter(),
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

    /// returns a string with replicas of the banned words including plural form this eases the bot hosting for the hoster
    /// normally they would have to manually type "badword,badwordbadword" etc. for the edge cases where users
    /// send bad words next to each other to avoid censor but this is automated now, including plural form (+s)
    /// other plural forms (e.g. +es) need to be added by the host in their .env (this can be added later but not urgent)
    private static String[] getBannedWordsArray() {

        var replicateAmountInt = EnvHelper.getREPLICATE_AMOUNT_INT();

        List<String> bannedWordsListTemp = new ArrayList<>(); //used for adding replicated words during iterations afterward all the items are added to real list
        List<String> bannedWordsList = new ArrayList<>(Arrays.asList(BANNED_WORDS_ARRAY_TEMP));

        log.debug("[getBannedWordsArray] Current new bannedWordsList: {}", bannedWordsList);
        log.debug("[getBannedWordsArray] replicateAmountInt {}", replicateAmountInt);

        for (var word : bannedWordsList) {
            var newBannedWord = "";

            //amount of time to replicate word which means: word wordword wordwordword wordwordwordword
            for (int i = 1; i <= replicateAmountInt; i++) {

                newBannedWord = word.repeat(i);
                bannedWordsListTemp.add(newBannedWord);

                log.debug("[getBannedWordsArray] Building word: {}", newBannedWord);
                log.debug("[getBannedWordsArray] Building list: {}", bannedWordsListTemp);

            }
        }

        for (var word : bannedWordsList) {
            var newBannedWord = "";

            //plural form +s
            for (int i = 1; i <= replicateAmountInt; i++) {

                newBannedWord = word.repeat(i) + "s";
                bannedWordsListTemp.add(newBannedWord);

                log.debug("[getBannedWordsArray] Building word plural: {}", newBannedWord);
                log.debug("[getBannedWordsArray] Building list plural: {}", bannedWordsListTemp);

            }
        }

        bannedWordsList.addAll(bannedWordsListTemp);

        log.debug("[getBannedWordsArray] Final built list: {}", bannedWordsListTemp);
        log.debug("[getBannedWordsArray] Final list with banned words + original from .env: {}", bannedWordsList);

        String[] bannedWordsArray = bannedWordsList.toArray(new String[0]);

        log.debug("[getBannedWordsArray] Final bannedWordsArray: {}", (Object) bannedWordsArray);

        return bannedWordsArray;
    }
}
