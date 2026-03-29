package va.rembot.database.dao;

import va.rembot.BotConfig;
import va.rembot.database.DataSource;
import va.rembot.database.models.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class MessageDao implements Dao<Message>{

    @Override
    public void create(Message message) {

        String query = "INSERT IGNORE INTO messages (discord_message_id, user_id, time_created) VALUES (?, ?, ?)";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement pStmt = conn.prepareStatement(query)){

            pStmt.setLong(1, message.discordMessageId());
            pStmt.setLong(2, message.discordId());
            pStmt.setTimestamp(3, message.timeCreated());
            pStmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Optional<Message> get(long id) {
        return Optional.empty();
    }

    @Override
    public List<Message> getAll() {
        return List.of();
    }

    public Optional<Message> getFirst(long discordId) {

        Message msgSpam;

        String query =
                "SELECT * FROM " +
                "(SELECT * FROM messages WHERE user_id = ? ORDER BY time_created DESC LIMIT " + BotConfig.ANTI_SPAM_WORDS_AMOUNT +") AS recent_messages " +
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

            msgSpam = new Message(discordMsgId, discordId, timeCreated);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return Optional.of(msgSpam);
    }

    public Optional<Message> getLatest(long discordId) {

        Message msgSpam;

        String query =
                "SELECT * FROM " +
                "(SELECT * FROM messages WHERE user_id = ? ORDER BY time_created DESC LIMIT " + BotConfig.ANTI_SPAM_WORDS_AMOUNT + ") AS recent_messages " +
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

            msgSpam = new Message(discordMsgId, discordId, timeCreated);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return Optional.of(msgSpam);
    }

    @Override
    public void update(Message message) {

    }

    @Override
    public void delete(Message message) {

    }

}
