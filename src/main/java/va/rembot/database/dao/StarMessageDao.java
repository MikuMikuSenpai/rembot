package va.rembot.database.dao;

import lombok.extern.slf4j.Slf4j;
import va.rembot.database.DataSource;
import va.rembot.database.models.StarMessage;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class StarMessageDao implements Dao<StarMessage>{
    @Override
    public void create(StarMessage starMessage) {
        String query = "INSERT IGNORE INTO starred_messages (discord_message_id, star_amount) VALUES (?, 0)";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement pStmt = conn.prepareStatement(query)){

            pStmt.setLong(1, starMessage.discordMsgId());
            pStmt.executeUpdate();

        } catch (SQLException e) {
            log.error("Could not insert new starMessage in DB table starred_messages.");
            log.error("Details: discordMsgId {}, starAmount {}", starMessage.discordMsgId(), starMessage.starAmount());
            log.error("Error: {}", e.getMessage());
        }
    }

    @Override
    public Optional<StarMessage> get(long MsgId) {
        String query = "SELECT * FROM starred_messages WHERE discord_message_id = ?";

        StarMessage starMsg = null;

        try (Connection conn = DataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)){

            ps.setLong(1, MsgId);

            ResultSet result = ps.executeQuery();
            long discordMsgId = 0L;
            int starAmount = 0;
            boolean isSent = false;
            long embedMsgId = 0L;

            while (result.next()) {
                discordMsgId = result.getLong(1);
                starAmount = result.getInt(2);
                isSent = result.getBoolean(3);
                embedMsgId = result.getLong(4);
            }

            starMsg = new StarMessage(discordMsgId, starAmount, isSent, embedMsgId);

        } catch (SQLException e) {
            log.error("Could not query star message based on discord message id.");
            log.error("Discord message id of user: {}", MsgId);
            log.error("Error: {}", e.getMessage());
        }

        return Optional.of(starMsg);
    }

    @Override
    public List<StarMessage> getAll() {
        return List.of();
    }

    @Override
    public void update(StarMessage starMessage) {
        String query = "UPDATE starred_messages SET star_amount = ?, is_sent = ?, embed_message_id = ? WHERE discord_message_id = ?";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement pStmt = conn.prepareStatement(query)){

            pStmt.setInt(1, starMessage.starAmount());
            pStmt.setBoolean(2, starMessage.isSent());
            pStmt.setLong(3, starMessage.embedMessageId());
            pStmt.setLong(4, starMessage.discordMsgId());

            pStmt.executeUpdate();

        } catch (SQLException e) {
            log.error("Could not update star amount.");
            log.error("Details: discordMsgId {}, starAmount {}", starMessage.discordMsgId(), starMessage.starAmount());
            log.error("Error: {}", e.getMessage());
        }
    }

    @Override
    public void delete(StarMessage starMessage) {

    }
}
