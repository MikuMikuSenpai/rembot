package va.rembot.database.models;

import java.sql.Timestamp;

public record StrikeSpam(long discordId, int amount, Timestamp mostRecentStrike) {

}
