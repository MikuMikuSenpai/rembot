package va.rembot;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
/// Helper method for loading ENV variables that are to be used within BotConfig
public class EnvHelper {

    private static final String REPLICATE_AMOUNT = System.getenv("REPLICATE_AMOUNT");
    @Getter
    public static final int REPLICATE_AMOUNT_INT = getInt();

    private static int getInt() {

        try {

            log.debug("[getInt] REPLICATE_AMOUNT_INT {} and REPLICATE_AMOUNT {}", REPLICATE_AMOUNT_INT, REPLICATE_AMOUNT);
            return Integer.parseInt(REPLICATE_AMOUNT);

        } catch (NumberFormatException e) {

            log.error("[getInt] REPLICATE_AMOUNT ENV VAR MISSING Check your .env file it is missing values use .env.example as a guide.");
            log.error("[getInt] REPLICATE_AMOUNT ENV VAR MISSING The bot cannot start until this is fixed.");
            log.error("[getInt] REPLICATE_AMOUNT ENV VAR MISSING error: {}", e.getMessage());
            System.exit(1); // force stop rembot

            return 0;
        }
    }
}
