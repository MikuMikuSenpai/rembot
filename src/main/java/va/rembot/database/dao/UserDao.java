package va.rembot.database.dao;

import lombok.extern.slf4j.Slf4j;
import va.rembot.database.DataSource;
import va.rembot.database.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class UserDao implements Dao<User>{

    @Override
    public void create(User user) {

        var id = user.discordId();
        String query = "INSERT IGNORE INTO users (discord_user_id) VALUES (?)";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)){

            ps.setLong(1, id);
            ps.executeUpdate();

        } catch (SQLException e) {
            log.error("Could not insert User in DB.");
            log.error("Details: discordId {}", user.discordId());
            log.error("Error: {}", e.getMessage());
        }

    }

    @Override
    public Optional<User> get(long id) {
        User user = null;
        String query = "SELECT * FROM users WHERE discord_user_id = ?";

        try (Connection conn = DataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(query)){

            ps.setLong(1, id);

            var result = ps.executeQuery();
            var userId = 0L;
            while (result.next()) {
                userId = result.getLong(1);
            }

            user = new User(userId);

        } catch (SQLException e) {
            log.error("Could not get user by id.");
            log.error("Details: discordId {}", id);
            log.error("Error: {}", e.getMessage());
        }

        return Optional.of(user);
    }

    @Override
    public List<User> getAll() {
        return List.of();
    }

    @Override
    public void update(User user) {

    }

    @Override
    public void delete(User user) {

    }
}
