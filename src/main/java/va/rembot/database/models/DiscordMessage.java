package va.rembot.database.models;

import java.sql.Timestamp;

public record DiscordMessage(long discordMessageId, long discordUserId, Timestamp timeCreated, String messageContent, String attachmentsLinks, boolean isEdited, String editedLogJumpUrl) {

}
