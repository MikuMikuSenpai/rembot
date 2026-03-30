package va.rembot;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
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
                    GatewayIntent.MESSAGE_CONTENT)
                    .disableCache( // this is to ignore the jda warnings, might need to enable these in the future
                            CacheFlag.VOICE_STATE,
                            CacheFlag.EMOJI,
                            CacheFlag.STICKER,
                            CacheFlag.SCHEDULED_EVENTS)
                    .build();
            printLogo();
        } catch (InvalidTokenException e) {
            log.error("Bot token is invalid, check your .env file.");
        } catch (Exception e){
            log.error("Something went wrong while starting the bot.");
            log.error(e.getMessage());
        }

        bot.addEventListener(new BotConfig());
    }

    private static void printLogo(){
        // credits: https://www.asciiart.eu/text-to-ascii-art
        // Update below configuration so that the logo can be updated correctly on new version releases
        // Current input text "REMBOT v0.0.0 PRERELEASE"
        // Font: standard Normal Normal 80
        // None
        // None
        // Whitespace Break | Trim Whitespace
        log.info(" ____  _____ __  __ ____   ___ _____          ___   ___   ___  ");
        log.info("|  _ \\| ____|  \\/  | __ ) / _ \\_   _| __   __/ _ \\ / _ \\ / _ \\ ");
        log.info("| |_) |  _| | |\\/| |  _ \\| | | || |   \\ \\ / / | | | | | | | | |");
        log.info("|  _ <| |___| |  | | |_) | |_| || |    \\ V /| |_| | |_| | |_| |");
        log.info("|_|_\\_\\_____|_|__|_|____/_\\___/_|_|  ___\\_/  \\___(_)___(_)___/ ");

        log.info("|  _ \\|  _ \\| ____|  _ \\| ____| |   | ____|  / \\  / ___|| ____|");
        log.info("| |_) | |_) |  _| | |_) |  _| | |   |  _|   / _ \\ \\___ \\|  _|  ");
        log.info("|  __/|  _ <| |___|  _ <| |___| |___| |___ / ___ \\ ___) | |___ ");
        log.info("|_|   |_| \\_\\_____|_| \\_\\_____|_____|_____/_/   \\_\\____/|_____|  ");
    }
}
