package va.rembot.database.models;

public record StarMessage(long discordMsgId, int starAmount, boolean isSent, long embedMessageId) {
}
