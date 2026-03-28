package va.rembot.database.dao;

import va.rembot.BotConfig;
import va.rembot.database.DataSource;
import va.rembot.database.models.MessageSpam;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class MessageSpamDao implements Dao<MessageSpam>{

    @Override
    public void create(MessageSpam messageSpam) {

        String query = "INSERT IGNORE INTO messages_spam (discord_message_id, user_id, time_created) VALUES (?, ?, ?)";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement pStmt = conn.prepareStatement(query)){

            pStmt.setLong(1, messageSpam.getDiscordMessageId());
            pStmt.setLong(2, messageSpam.getDiscordId());
            pStmt.setTimestamp(3, messageSpam.getTimeCreated());
            pStmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Optional<MessageSpam> get(long id) {
        return Optional.empty();
    }

    @Override
    public List<MessageSpam> getAll() {
        return List.of();
    }

    public Optional<MessageSpam> getFirst(long discordId) {

        MessageSpam msgSpam;

        String query =
                "SELECT * FROM " +
                "(SELECT * FROM messages_spam WHERE user_id = ? ORDER BY time_created DESC LIMIT " + BotConfig.ANTI_SPAM_WORDS_AMOUNT +") AS recent_messages " +
                "ORDER BY time_created ASC LIMIT 1";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)){

            ps.setLong(1, discordId);

            var result = ps.executeQuery();
            var discordMsgId = 0L;
            Timestamp timeCreated = null;

            while (result.next()) {
                discordMsgId = result.getLong(1);
                discordId = result.getLong(2);
                timeCreated = result.getTimestamp(3);
            }

            msgSpam = new MessageSpam(discordMsgId, discordId, timeCreated);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return Optional.of(msgSpam);
    }

    public Optional<MessageSpam> getLatest(long discordId) {

        MessageSpam msgSpam;

        String query =
                "SELECT * FROM " +
                "(SELECT * FROM messages_spam WHERE user_id = ? ORDER BY time_created DESC LIMIT " + BotConfig.ANTI_SPAM_WORDS_AMOUNT + ") AS recent_messages " +
                "LIMIT 1";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)){

            ps.setLong(1, discordId);

            var result = ps.executeQuery();
            var discordMsgId = 0L;
            Timestamp timeCreated = null;

            while (result.next()) {
                discordMsgId = result.getLong(1);
                discordId = result.getLong(2);
                timeCreated = result.getTimestamp(3);
            }

            msgSpam = new MessageSpam(discordMsgId, discordId, timeCreated);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return Optional.of(msgSpam);
    }

    @Override
    public void update(MessageSpam messageSpam) {

    }

    @Override
    public void delete(MessageSpam messageSpam) {

    }

}
