package va.rembot.database.models;

import java.sql.Timestamp;

public record DiscordMessage(long discordMessageId, long discordId, Timestamp timeCreated, String messageContent, String attachmentsLinks) {

}
