package va.rembot.database.dao;

import va.rembot.database.DataSource;
import va.rembot.database.models.StrikeSpam;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class StrikeSpamDao implements Dao<StrikeSpam> {

    @Override
    public void create(StrikeSpam strikeSpam) {

        String query = "INSERT IGNORE INTO strikes_spam (discord_user_id, amount, most_recent_given) VALUES (?, 0, ?)";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement pStmt = conn.prepareStatement(query)){

            pStmt.setLong(1, strikeSpam.discordId());
            pStmt.setTimestamp(2, strikeSpam.mostRecentStrike());
            pStmt.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    public Optional<StrikeSpam> get(long id) {
        return Optional.empty();
    }

    @Override
    public List<StrikeSpam> getAll() {
        return List.of();
    }

    public Optional<StrikeSpam> getAmount(long discordId) {

        StrikeSpam strikeSpam;

        String query = "SELECT * FROM strikes_spam WHERE discord_user_id = ?";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)){

            ps.setLong(1, discordId);

            var result = ps.executeQuery();
            var amount = 0;
            Timestamp mostRecentGiven = null;

            while (result.next()) {
                amount = result.getInt(2);
                mostRecentGiven = result.getTimestamp(3);
            }

            strikeSpam = new StrikeSpam(discordId, amount, mostRecentGiven);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        return Optional.of(strikeSpam);
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
            throw new RuntimeException(e);
        }
    }

    public void updateAmountToZero(StrikeSpam strikeSpam) {

        String query = "UPDATE strikes_spam SET amount = 0, most_recent_given = ? WHERE discord_user_id = ?";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)){

            ps.setTimestamp(1, strikeSpam.mostRecentStrike());
            ps.setLong(2, strikeSpam.discordId());

            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(StrikeSpam strikeSpam) {

    }
}
