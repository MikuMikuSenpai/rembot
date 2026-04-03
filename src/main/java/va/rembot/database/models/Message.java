package va.rembot.database.models;

import java.sql.Timestamp;

public record Message(long discordMessageId, long discordId, Timestamp timeCreated, String messageContent) {

}
