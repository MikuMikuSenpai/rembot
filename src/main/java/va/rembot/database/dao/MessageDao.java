package va.rembot.database.dao;

import lombok.extern.slf4j.Slf4j;
import va.rembot.BotConfig;
import va.rembot.database.DataSource;
import va.rembot.database.models.Message;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MessageDao implements Dao<Message>{

    @Override
    public void create(Message message) {

        String query = "INSERT IGNORE INTO messages (discord_message_id, user_id, time_created, message_content) VALUES (?, ?, ?, ?)";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement pStmt = conn.prepareStatement(query)){

            pStmt.setLong(1, message.discordMessageId());
            pStmt.setLong(2, message.discordId());
            pStmt.setTimestamp(3, message.timeCreated());
            pStmt.setString(4, message.messageContent());
            pStmt.executeUpdate();

        } catch (SQLException e) {
            log.error("Could not insert Message in DB.");
            log.error("Message details: discordMessageId {}, discordId {}, timeCreated {}, messageContent {}", message.discordMessageId(), message.discordId(), message.timeCreated(), message.messageContent());
            log.error("Error: {}", e.getMessage());;
        }

    }

    @Override
    public Optional<Message> get(long discordMsgId) {

        Message msg = null;

        String query = "SELECT * FROM messages WHERE discord_message_id = ?";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)){

            ps.setLong(1, discordMsgId);

            var result = ps.executeQuery();
            var discordUserId = 0L;
            Timestamp timeCreated = null;
            var messageContent = "";

            while (result.next()) {
                discordUserId = result.getLong(2);
                timeCreated = result.getTimestamp(3);
                messageContent = result.getString(4);
            }

            msg = new Message(discordMsgId, discordUserId, timeCreated, messageContent);

        } catch (SQLException e) {
            log.error("Could not get message.");
            log.error("Discord message id: {}", discordMsgId);
            log.error("Error: {}", e.getMessage());
        }

        return Optional.of(msg);

    }

    @Override
    public List<Message> getAll() {
        return List.of();
    }

    public Optional<Message> getFirst(long discordId) {

        Message msgSpam = null;

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
            var messageContent = "";

            while (result.next()) {
                discordMsgId = result.getLong(1);
                discordId = result.getLong(2);
                timeCreated = result.getTimestamp(3);
                messageContent = result.getString(4);
            }

            msgSpam = new Message(discordMsgId, discordId, timeCreated, messageContent);

        } catch (SQLException e) {
            log.error("Could not get first message for spam detection.");
            log.error("Discord id of user: {}", discordId);
            log.error("Error: {}", e.getMessage());
        }

        return Optional.of(msgSpam);
    }

    public Optional<Message> getLatest(long discordId) {

        Message msgSpam = null;

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
            String messageContent = "";

            while (result.next()) {
                discordMsgId = result.getLong(1);
                discordId = result.getLong(2);
                timeCreated = result.getTimestamp(3);
                messageContent = result.getString(4);
            }

            msgSpam = new Message(discordMsgId, discordId, timeCreated, messageContent);

        } catch (SQLException e) {
            log.error("Could not get latest message for spam detection.");
            log.error("Discord id of user: {}", discordId);
            log.error("Error: {}", e.getMessage());
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
