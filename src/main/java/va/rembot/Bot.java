package va.rembot;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

@Slf4j
/// ONLY starts the bot nothing else.
public class Bot {

    private static final String BOT_TOKEN = BotConfig.BOT_TOKEN;

    public static void main(String[] args) {

        JDA bot = null;
        try {
            bot = JDABuilder.createDefault(
                    BOT_TOKEN,
                    GatewayIntent.GUILD_MESSAGES,
                    GatewayIntent.MESSAGE_CONTENT,
                    GatewayIntent.GUILD_MESSAGE_REACTIONS,
                    GatewayIntent.GUILD_MEMBERS)
                    .disableCache( // this is to ignore the jda warnings, might need to enable these in the future
                            CacheFlag.VOICE_STATE,
                            CacheFlag.EMOJI,
                            CacheFlag.STICKER,
                            CacheFlag.SCHEDULED_EVENTS,
                            CacheFlag.SOUNDBOARD_SOUNDS)
                    .setMemberCachePolicy(MemberCachePolicy.ALL)//if server becomes big change this
                    .build();
            printLogo();
        } catch (InvalidTokenException e) {
            log.error("Bot token is invalid, check your .env file.");
        } catch (Exception e){
            log.error("Something went wrong while starting the bot.");
            log.error(e.getMessage());
        }

        bot.getApplicationManager()
                .setDescription("Bot made so i can mess around\n\nhttps://github.com/MikuMikuSenpai/rembot")
                .queue();

        bot.addEventListener(new BotConfig());
    }

    private static void printLogo(){
        // credits: https://www.asciiart.eu/text-to-ascii-art
        // input_text: "REMBOT" / only project name is ASCII art for now
        // Font: standard Normal Normal 80
        // None 0 0
        // None standard 14pt
        // Whitespace Break | Trim Whitespace
        log.info(" ____  _____ __  __ ____   ___ _____");
        log.info("|  _ \\| ____|  \\/  | __ ) / _ \\_   _|");
        log.info("| |_) |  _| | |\\/| |  _ \\| | | || |");
        log.info("|  _ <| |___| |  | | |_) | |_| || |");
        log.info("|_|_\\_\\_____|_|__|_|____/_\\___/_|_| PRERELEASE");
    }
}
