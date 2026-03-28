package va.rembot.database.models;

import lombok.Data;

import java.sql.Timestamp;

@Data
public class MessageSpam {

    private final long discordMessageId;
    private final long discordId;
    private final Timestamp timeCreated;

}
