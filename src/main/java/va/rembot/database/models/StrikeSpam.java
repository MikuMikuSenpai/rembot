package va.rembot.database.models;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class StrikeSpam {

    private final long discordId;
    private final int amount;
    private final Timestamp mostRecentStrike;

}
