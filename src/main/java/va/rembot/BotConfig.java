package va.rembot;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import va.rembot.commands.non_slash.PingPong;
import va.rembot.commands.slash.admin.*;
import va.rembot.moderation.AntiSpamFilter;
import va.rembot.moderation.AutoDeleteDiscordInviteLinks;
import va.rembot.moderation.word_filter.BannedWordsFilter;
import va.rembot.other.highlight.HighlightedMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/// All global variables should be here and configuration is done here
@Slf4j
public class BotConfig extends ListenerAdapter {

    public static final String BOT_TOKEN = System.getenv("BOT_TOKEN");
    public static final String IMAGE_TAG = System.getenv("IMAGE_TAG");
    public static final String GUILD_ID = System.getenv("GUILD_ID");
    private static final String MOD_ROLE_ID = System.getenv("MOD_ROLE_ID");
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
    public static final String ANTI_SPAM_MESSAGES_AMOUNT = System.getenv("ANTI_SPAM_MESSAGES_AMOUNT");
    private static final String ANTI_SPAM_TIME_AMOUNT = System.getenv("ANTI_SPAM_TIME_AMOUNT");
    @Getter
    private static int antiSpamTimeAmountInt = 0;
    private static final String ANTI_SPAM_MUTE_AMOUNT = System.getenv("ANTI_SPAM_MUTE_AMOUNT");
    @Getter
    private static int antiSpamMuteAmountInt = 0;
    private static final String ANTI_SPAM_STRIKE_AMOUNT = System.getenv("ANTI_SPAM_STRIKE_AMOUNT");
    @Getter
    private static int antiSpamStrikeAmountInt = 0;
    private static final String SUBSTITUTE_BANNED_WORD_CHECK_AMOUNT = System.getenv("SUBSTITUTE_BANNED_WORD_CHECK_AMOUNT");
    @Getter
    private static int substituteBannedWordCheckAmountInt = 0;
    private static final String HIGHLIGHT_STAR_THRESHOLD = System.getenv("HIGHLIGHT_STAR_THRESHOLD");
    @Getter
    private static int highlightStarThresholdInt = 0;
    private static final String ALLOWED_AMOUNT_SUBSTITUTE_CHARACTERS_PER_MESSAGE = System.getenv("ALLOWED_AMOUNT_SUBSTITUTE_CHARACTERS_PER_MESSAGE");
    @Getter
    private static int allowedAmountSubstituteCharactersPerMessage = 0;

    @Override
    public void onReady(ReadyEvent event) {

        JDA bot = event.getJDA();

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

            if (antiSpamTimeAmountInt <= 0) {
                log.error("[onReady] ANTI_SPAM_TIME_AMOUNT is 0 or a negative number, it must be a POSITIVE number. Check your .env file.");
                log.error("[onReady] rembot can not start without fixing this issue first.");
                bot.shutdown();
            }
        } catch (NumberFormatException e) {

            log.error("[onReady] ANTI_SPAM_TIME_AMOUNT ENV VAR MISSING Check your .env file it is missing values use .env.example as a guide.");
            log.error("[onReady] ANTI_SPAM_TIME_AMOUNT ENV VAR MISSING The bot cannot start until this is fixed.");
            log.error("[onReady] ANTI_SPAM_TIME_AMOUNT ENV VAR MISSING error: {}", e.getMessage());
            bot.shutdown();
        }

        try {
            antiSpamMuteAmountInt = Integer.parseInt(ANTI_SPAM_MUTE_AMOUNT);

            if (antiSpamMuteAmountInt <= 0) {
                log.error("[onReady] ANTI_SPAM_MUTE_AMOUNT is 0 or a negative number, it must be a POSITIVE number. Check your .env file.");
                log.error("[onReady] rembot can not start without fixing this issue first.");
                bot.shutdown();
            }
        } catch (NumberFormatException e) {

            log.error("[onReady] ANTI_SPAM_MUTE_AMOUNT ENV VAR MISSING Check your .env file it is missing values use .env.example as a guide.");
            log.error("[onReady] ANTI_SPAM_MUTE_AMOUNT ENV VAR MISSING The bot cannot start until this is fixed.");
            log.error("[onReady] ANTI_SPAM_MUTE_AMOUNT ENV VAR MISSING error: {}", e.getMessage());
            bot.shutdown();
        }

        try {
            antiSpamStrikeAmountInt = Integer.parseInt(ANTI_SPAM_STRIKE_AMOUNT);

            if (antiSpamStrikeAmountInt <= 0)
                log.warn("[onReady] ANTI_SPAM_STRIKE_AMOUNT is 0 or a negative number, this means that people will be instantly banned if spamming this is probably NOT what you want. Check your .env file.");
        } catch (NumberFormatException e) {

            log.error("[onReady] ANTI_SPAM_STRIKE_AMOUNT ENV VAR MISSING Check your .env file it is missing values use .env.example as a guide.");
            log.error("[onReady] ANTI_SPAM_STRIKE_AMOUNT ENV VAR MISSING The bot cannot start until this is fixed.");
            log.error("[onReady] ANTI_SPAM_STRIKE_AMOUNT ENV VAR MISSING error: {}", e.getMessage());
            bot.shutdown();
        }

        try {
            substituteBannedWordCheckAmountInt = Integer.parseInt(SUBSTITUTE_BANNED_WORD_CHECK_AMOUNT);

            if (substituteBannedWordCheckAmountInt <= 0)
                log.warn("[onReady] SUBSTITUTE_BANNED_WORD_CHECK_AMOUNT is 0 or a negative number, this breaks banned word check. Change it to a positive number. Check your .env file.");
        } catch (NumberFormatException e) {

            log.error("[onReady] SUBSTITUTE_BANNED_WORD_CHECK_AMOUNT ENV VAR MISSING Check your .env file it is missing values use .env.example as a guide.");
            log.error("[onReady] SUBSTITUTE_BANNED_WORD_CHECK_AMOUNT ENV VAR MISSING The bot cannot start until this is fixed.");
            log.error("[onReady] SUBSTITUTE_BANNED_WORD_CHECK_AMOUNT ENV VAR MISSING error: {}", e.getMessage());
            bot.shutdown();
        }

        try {
            highlightStarThresholdInt = Integer.parseInt(HIGHLIGHT_STAR_THRESHOLD);
        } catch (NumberFormatException e) {

            log.error("[onReady] HIGHLIGHT_STAR_THRESHOLD ENV VAR MISSING Check your .env file it is missing values use .env.example as a guide.");
            log.error("[onReady] HIGHLIGHT_STAR_THRESHOLD ENV VAR MISSING The bot cannot start until this is fixed.");
            log.error("[onReady] HIGHLIGHT_STAR_THRESHOLD ENV VAR MISSING error: {}", e.getMessage());
            bot.shutdown();
        }

        try {
            allowedAmountSubstituteCharactersPerMessage = Integer.parseInt(ALLOWED_AMOUNT_SUBSTITUTE_CHARACTERS_PER_MESSAGE);

            if (allowedAmountSubstituteCharactersPerMessage <= 0)
                log.warn("[onReady] ALLOWED_AMOUNT_SUBSTITUTE_CHARACTERS_PER_MESSAGE is 0 or a negative number, this breaks banned word check. Change it to a positive number. Check your .env file.");
        } catch (NumberFormatException e) {

            log.error("[onReady] ALLOWED_AMOUNT_SUBSTITUTE_CHARACTERS_PER_MESSAGE ENV VAR MISSING Check your .env file it is missing values use .env.example as a guide.");
            log.error("[onReady] ALLOWED_AMOUNT_SUBSTITUTE_CHARACTERS_PER_MESSAGE ENV VAR MISSING The bot cannot start until this is fixed.");
            log.error("[onReady] ALLOWED_AMOUNT_SUBSTITUTE_CHARACTERS_PER_MESSAGE ENV VAR MISSING error: {}", e.getMessage());
            bot.shutdown();
        }

        if (bot.getGuilds().size() > 1) {
            log.error("[onReady] rembot can only be connected to one server at a time. Fix this issue by only adding the bot to your main server.");
            log.error("[onReady] Current connected guilds/servers: {}", bot.getGuilds());
            log.error("[onReady] rembot can not start without fixing this issue first.");
            bot.shutdown();
        }

        try {
            bot.getGuildById(GUILD_ID).getJDA();
        } catch (NullPointerException e) {
            log.error("[onReady] GUILD_ID {} is not valid, fix your .env file.", GUILD_ID);
            log.error("[onReady] rembot can not start without fixing this issue first.");
            bot.shutdown();
        } catch (IllegalArgumentException e) {
            log.error("[onReady] GUILD_ID may not be empty, fix your .env file.");
            log.error("[onReady] rembot can not start without fixing this issue first.");
            bot.shutdown();
        }

        try {
            bot.getRoleById(MOD_ROLE_ID).getJDA();
        } catch (NullPointerException e) {
            log.error("[onReady] MOD_ROLE_ID {} is not valid, fix your .env file.", MOD_ROLE_ID);
            log.error("[onReady] rembot can not start without fixing this issue first.");
            bot.shutdown();
        } catch (IllegalArgumentException e) {}

        try {
            bot.getChannelById(TextChannel.class, LOG_CHANNEL_ID).getJDA();
        } catch (NullPointerException e) {
            log.error("[onReady] LOG_CHANNEL_ID {} is not valid, fix your .env file.", LOG_CHANNEL_ID);
            log.error("[onReady] rembot can not start without fixing this issue first.");
            bot.shutdown();
        } catch (IllegalArgumentException e) {
            log.error("[onReady] LOG_CHANNEL_ID may not be empty, fix your .env file.");
            log.error("[onReady] rembot can not start without fixing this issue first.");
            bot.shutdown();
        }

        try {
            bot.getChannelById(TextChannel.class, DARWIN_CHANNEL_ID).getJDA();
        } catch (NullPointerException e) {
            log.error("[onReady] DARWIN_CHANNEL_ID {} is not valid, fix your .env file.", DARWIN_CHANNEL_ID);
            log.error("[onReady] rembot can not start without fixing this issue first.");
            bot.shutdown();
        } catch (IllegalArgumentException e) {
            log.error("[onReady] DARWIN_CHANNEL_ID may not be empty, fix your .env file.");
            log.error("[onReady] rembot can not start without fixing this issue first.");
            bot.shutdown();
        }

        //we need to check this cus itll break SQL shit if we dont
        try {
            if (Integer.parseInt(ANTI_SPAM_MESSAGES_AMOUNT) <= 0) {
                log.error("[onReady] ANTI_SPAM_MESSAGES_AMOUNT is 0 or a negative number, it must be a POSITIVE number. Check your .env file.");
                log.error("[onReady] rembot can not start without fixing this issue first.");
                bot.shutdown();
            }
        } catch (NumberFormatException e) {
            log.error("[onReady] ANTI_SPAM_MESSAGES_AMOUNT may not be empty.");
            log.error("[onReady] rembot can not start without fixing this issue first.");
            bot.shutdown();
        }

        if (EnvHelper.getREPLICATE_AMOUNT_INT() <= 0)
            log.warn("[onReady] REPLICATE_AMOUNT is set to 0 or a negative number, this means that banned words are not replicated this is probably not what you want. Check your .env file.");

        bot.addEventListener(
                new Ban(),
                new Unban(),
                new Kick(),
                new Mute(),
                new Unmute(),
                new BannedWordsFilter(),
                new AntiSpamFilter(),
                new AutoDeleteDiscordInviteLinks(),
                new HighlightedMessage(),
                // non slash
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
                                .addOption(OptionType.INTEGER, "hours", "The amount of hours to be muted for.", false),
                        Commands.slash("unmute", "Unmute someone.")
                                .setDefaultPermissions(DefaultMemberPermissions.enabledFor(Permission.MODERATE_MEMBERS))
                                .addOption(OptionType.USER, "username", "The user to be unmuted.", true)
                                .addOption(OptionType.STRING, "reason", "Reason for unmute.", false))
                .queue(success -> log.info("Successfully loaded all slash commands."));
    }

    private static String[] getBannedWordsArray() {

        int replicateAmountInt = EnvHelper.getREPLICATE_AMOUNT_INT();

        List<String> bannedWordsListTemp = new ArrayList<>();
        List<String> bannedWordsList = new ArrayList<>(Arrays.asList(BANNED_WORDS_ARRAY_TEMP));

        log.debug("[getBannedWordsArray] Current new bannedWordsList: {}", bannedWordsList);
        log.debug("[getBannedWordsArray] replicateAmountInt {}", replicateAmountInt);

        for (String word : bannedWordsList) {
            StringBuilder newBannedWord = new StringBuilder();

            //amount of time to replicate word which means: word wordword wordwordword wordwordwordword
            for (int i = 1; i <= replicateAmountInt; i++) {

                newBannedWord.setLength(0);
                newBannedWord.append(word.repeat(i));
                bannedWordsListTemp.add(newBannedWord.toString());

                log.debug("[getBannedWordsArray] Building word: {}", newBannedWord);
                log.debug("[getBannedWordsArray] Building list: {}", bannedWordsListTemp);
            }
        }

        for (String word : bannedWordsList) {
            StringBuilder newBannedWord = new StringBuilder();

            //plural form +s
            for (int i = 1; i <= replicateAmountInt; i++) {

                newBannedWord.setLength(0);
                newBannedWord.append(word.repeat(i) + "s");
                bannedWordsListTemp.add(newBannedWord.toString());

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
