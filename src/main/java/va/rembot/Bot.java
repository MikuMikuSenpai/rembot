package va.rembot;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;

@Slf4j
/// ONLY starts the bot nothing else.
public class Bot {

    private static final String BOT_TOKEN = BotConfig.BOT_TOKEN;

    public static void main(String[] args) {

        JDA bot = null;
        try {
            bot = JDABuilder.createDefault(BOT_TOKEN).build().awaitReady();
        } catch (InvalidTokenException e) {
            log.error("Bot token is invalid, check your .env file.");
        } catch (InterruptedException e) {
            log.error("This thread got interrupted while starting the bot.");
            log.error(e.getMessage());
        } catch (Exception e){
            log.error("Something went wrong while starting the bot.");
            log.error(e.getMessage());
        }

        bot.addEventListener(new BotConfig());

    }
}
