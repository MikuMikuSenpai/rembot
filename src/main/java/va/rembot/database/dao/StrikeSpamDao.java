package va.rembot.database.dao;

import lombok.extern.slf4j.Slf4j;
import va.rembot.database.DataSource;
import va.rembot.database.models.StrikeSpam;

import java.sql.*;
import java.util.List;
import java.util.Optional;

@Slf4j
public class StrikeSpamDao implements Dao<StrikeSpam> {

    @Override
    public void create(StrikeSpam strikeSpam) {

        String query = "INSERT IGNORE INTO strikes_spam (discord_user_id, amount, most_recent_given) VALUES (?, ?, ?)";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement pStmt = conn.prepareStatement(query)) {

            pStmt.setLong(1, strikeSpam.discordId());
            pStmt.setInt(2, strikeSpam.amount());
            pStmt.setTimestamp(3, strikeSpam.mostRecentStrike());
            pStmt.executeUpdate();

        } catch (SQLException e) {
            log.error("[create] Could not insert new StrikeSpam in DB.");
            log.error("[create] Details: {}", strikeSpam);
            log.error("[create] Error: {}", e.getMessage());
        }
    }

    @Override
    public Optional<StrikeSpam> get(long discordUserId) {
        StrikeSpam strikeSpam = null;

        String query = "SELECT * FROM strikes_spam WHERE discord_user_id = ?";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)) {

            ps.setLong(1, discordUserId);
            ResultSet result = ps.executeQuery();

            int amount = 0;
            Timestamp mostRecentGiven = null;

            while (result.next()) {
                amount = result.getInt(2);
                mostRecentGiven = result.getTimestamp(3);
            }

            strikeSpam = new StrikeSpam(discordUserId, amount, mostRecentGiven);

        } catch (SQLException e) {
            log.error("[get] Could not get strikeSpam from DB");
            log.error("[get] Discord id of user: {}", discordUserId);
            log.error("[get] Error: {}", e.getMessage());
        }

        return Optional.of(strikeSpam);
    }

    @Override
    public List<StrikeSpam> getAll() {
        return List.of();
    }

    @Override
    public void update(StrikeSpam strikeSpam) {

        String query = "UPDATE strikes_spam SET amount = ?, most_recent_given = ? WHERE discord_user_id = ?";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)){

            ps.setInt(1, strikeSpam.amount());
            ps.setTimestamp(2, strikeSpam.mostRecentStrike());
            ps.setLong(3, strikeSpam.discordId());

            ps.executeUpdate();

        } catch (SQLException e) {
            log.error("[update] Could not update strikeSpam with new amount and timestamp for spam detection.");
            log.error("[update] StrikeSpam details: {}", strikeSpam);
            log.error("[update] Error: {}", e.getMessage());
        }
    }

    @Override
    public void delete(StrikeSpam strikeSpam) {

    }
}
