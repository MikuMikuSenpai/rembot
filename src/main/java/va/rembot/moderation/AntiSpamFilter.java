package va.rembot.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.BotConfig;

import java.sql.*;

@Slf4j
public class AntiSpamFilter extends ListenerAdapter {

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        try {

            //TODO TEMP LIKE THIS WILL PROB DO DAO DESIGN PATTERN CUS THIS IS FRANKENSTEIN AF (AND THIS IS A SIMPLE QUERY)

            var msgCreated = event.getMessage().getTimeCreated().toInstant().toEpochMilli();

            Connection conn = DriverManager.getConnection("jdbc:mysql://db:3306/" + BotConfig.MYSQL_DATABASE, "root", BotConfig.MYSQL_ROOT_PASSWORD);

            String insertUserQuery = "INSERT IGNORE INTO users (discord_user_id) VALUES (?)";
            String insertMessageQuery = "INSERT IGNORE INTO messages_spam (discord_message_id, user_id, time_created) VALUES (?, ?, ?)";

            PreparedStatement firstQuery = conn.prepareStatement(insertUserQuery);
            PreparedStatement secondQuery = conn.prepareStatement(insertMessageQuery);

            firstQuery.setLong(1, event.getMember().getIdLong());

            secondQuery.setLong(1, event.getMessageIdLong());
            secondQuery.setLong(2, event.getMember().getIdLong());
            secondQuery.setTimestamp(3, new Timestamp(msgCreated));

            firstQuery.executeUpdate();
            log.debug("[onMessageReceived] firstQuery executed: inserted user's Discord ID in users table if not yet in it.");
            secondQuery.executeUpdate();
            log.debug("[onMessageReceived] secondQuery executed: inserted message in messages_spam table.");

            firstQuery.close();
            secondQuery.close();
            conn.close();

            log.debug("[onMessageReceived] Closed all statements and connection.");

        } catch (SQLException ex) {
            log.error("SQLException: {}", ex.getMessage());
            log.error("SQLState: {}", ex.getSQLState());
            log.error("VendorError: {}", ex.getErrorCode());
        }
    }
}
