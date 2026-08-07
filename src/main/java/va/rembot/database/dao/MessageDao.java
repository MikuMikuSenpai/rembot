package va.rembot.database.dao;

import lombok.extern.slf4j.Slf4j;
import va.rembot.BotConfig;
import va.rembot.database.DataSource;
import va.rembot.database.models.DiscordMessage;

import java.sql.*;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MessageDao implements Dao<DiscordMessage> {

    @Override
    public void create(DiscordMessage message) {

        String query = "INSERT IGNORE INTO messages (discord_message_id, user_id, time_created, message_content, attachments_links) VALUES (?, ?, ?, ?, ?)";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement pStmt = conn.prepareStatement(query)) {

            if (message.messageContent().length() > 4000) {
                log.error("Message content is too long. Message could not be stored in the database.");
                throw new SQLException();
            }

            if (message.attachmentsLinks().length() > 5000) {
                log.error("Attachment links are too long. Message could not be stored in the database.");
                throw new SQLException();
            }

            pStmt.setLong(1, message.discordMessageId());
            pStmt.setLong(2, message.discordId());
            pStmt.setTimestamp(3, message.timeCreated());
            pStmt.setString(4, message.messageContent());
            pStmt.setString(5, message.attachmentsLinks());
            pStmt.executeUpdate();

        } catch (SQLException e) {
            log.error("Could not insert Message in DB.");
            log.error("Message details: discordMessageId {}, discordId {}, timeCreated {}, messageContent {}, attachmentsLinks {}",
                    message.discordMessageId(), message.discordId(), message.timeCreated(), message.messageContent(), message.attachmentsLinks());
            log.error("Error: {}", e.getMessage());;
        }

    }

    @Override
    public Optional<DiscordMessage> get(long discordMsgId) {

        DiscordMessage msg = null;

        String query = "SELECT * FROM messages WHERE discord_message_id = ?";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setLong(1, discordMsgId);

            ResultSet result = ps.executeQuery();
            long discordUserId = 0L;
            Timestamp timeCreated = null;
            String messageContent = "";
            String attachmentLinks = "";

            while (result.next()) {
                discordUserId = result.getLong(2);
                timeCreated = result.getTimestamp(3);
                messageContent = result.getString(4);
                attachmentLinks = result.getString(5);
            }

            msg = new DiscordMessage(discordMsgId, discordUserId, timeCreated, messageContent, attachmentLinks);

        } catch (SQLException e) {
            log.error("Could not get message.");
            log.error("Discord message id: {}", discordMsgId);
            log.error("Error: {}", e.getMessage());
        }

        return Optional.ofNullable(msg);

    }

    @Override
    public List<DiscordMessage> getAll() {
        return List.of();
    }

    public Optional<DiscordMessage> getFirst(long discordId) {

        DiscordMessage msgSpam = null;

        String query =
                "SELECT * FROM " +
                "(SELECT * FROM messages WHERE user_id = ? ORDER BY time_created DESC LIMIT " + BotConfig.ANTI_SPAM_MESSAGES_AMOUNT +") AS recent_messages " +
                "ORDER BY time_created ASC LIMIT 1";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setLong(1, discordId);

            ResultSet result = ps.executeQuery();
            long discordMsgId = 0L;
            Timestamp timeCreated = null;
            String messageContent = "";
            String attachmentLinks = "";

            while (result.next()) {
                discordMsgId = result.getLong(1);
                discordId = result.getLong(2);
                timeCreated = result.getTimestamp(3);
                messageContent = result.getString(4);
                attachmentLinks = result.getString(5);
            }

            msgSpam = new DiscordMessage(discordMsgId, discordId, timeCreated, messageContent, attachmentLinks);

        } catch (SQLException e) {
            log.error("Could not get first message for spam detection.");
            log.error("Discord id of user: {}", discordId);
            log.error("Error: {}", e.getMessage());
        }

        return Optional.of(msgSpam);
    }

    public Optional<DiscordMessage> getLatest(long discordId) {

        DiscordMessage msgSpam = null;

        String query =
                "SELECT * FROM " +
                "(SELECT * FROM messages WHERE user_id = ? ORDER BY time_created DESC LIMIT " + BotConfig.ANTI_SPAM_MESSAGES_AMOUNT + ") AS recent_messages " +
                "LIMIT 1";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setLong(1, discordId);

            ResultSet result = ps.executeQuery();
            long discordMsgId = 0L;
            Timestamp timeCreated = null;
            String messageContent = "";
            String attachmentLinks = "";

            while (result.next()) {
                discordMsgId = result.getLong(1);
                discordId = result.getLong(2);
                timeCreated = result.getTimestamp(3);
                messageContent = result.getString(4);
                attachmentLinks = result.getString(5);
            }

            msgSpam = new DiscordMessage(discordMsgId, discordId, timeCreated, messageContent, attachmentLinks);

        } catch (SQLException e) {
            log.error("Could not get latest message for spam detection.");
            log.error("Discord id of user: {}", discordId);
            log.error("Error: {}", e.getMessage());
        }

        return Optional.of(msgSpam);
    }

    @Override
    public void update(DiscordMessage message) {

    }

    @Override
    public void delete(DiscordMessage message) {

    }
}
