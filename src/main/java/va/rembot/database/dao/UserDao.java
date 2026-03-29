package va.rembot.database.dao;

import va.rembot.database.DataSource;
import va.rembot.database.models.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

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
            throw new RuntimeException(e);
        }

    }

    @Override
    public Optional<User> get(long id) {
        User user;
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
            throw new RuntimeException(e);
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
