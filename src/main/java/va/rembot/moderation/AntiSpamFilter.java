package va.rembot.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.BotConfig;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
public class AntiSpamFilter extends ListenerAdapter {

    private final int ANTI_SPAM_TIME_AMOUNT = BotConfig.getAntiSpamTimeAmountInt();
    private final int ANTI_SPAM_STRIKE_AMOUNT = BotConfig.getAntiSpamStrikeAmountInt();
    private final int ANTI_SPAM_MUTE_AMOUNT = BotConfig.getAntiSpamMuteAmountInt();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (event.getMember().getUnsortedRoles().contains(BotConfig.getModRoleIdLong())) return;

        try {

            //TODO TEMP LIKE THIS WILL PROB DO DAO DESIGN PATTERN CUS THIS IS FRANKENSTEIN AF (AND THIS IS A SIMPLE QUERY)
            // also use try w resources and other things oracle advises but first create the frankenstein we have but working
            // https://docs.oracle.com/javase/tutorial/jdbc/basics/index.html

            var msgCreated = event.getMessage().getTimeCreated().toInstant().toEpochMilli();
            var user = event.getMember();
            var discordId = event.getMember().getIdLong();
            var usrSnowflake = UserSnowflake.fromId(discordId);
            var discordMsgId = event.getMessageIdLong();
            Timestamp timeFirstMsgCreated = null;
            Timestamp timeLastMsgCreated = null;

            Connection conn = DriverManager.getConnection("jdbc:mysql://db:3306/" + BotConfig.MYSQL_DATABASE, "root", BotConfig.MYSQL_ROOT_PASSWORD);

            String insertUserQuery = "INSERT IGNORE INTO users (discord_user_id) VALUES (?)";
            String insertMessageQuery = "INSERT IGNORE INTO messages_spam (discord_message_id, user_id, time_created) VALUES (?, ?, ?)";
            String insertStrikesQuery = "INSERT IGNORE INTO strikes_spam (discord_user_id, amount, most_recent_given) VALUES (?, 0, ?)";

            String getAmountStrikesQuery = "SELECT * FROM strikes_spam WHERE discord_user_id = ?";
            String getFirstMessageQuery =
                    "SELECT * FROM " +
                    "(SELECT * FROM messages_spam WHERE user_id = ? ORDER BY time_created DESC LIMIT " + BotConfig.ANTI_SPAM_WORDS_AMOUNT +") AS recent_messages " +
                    "ORDER BY time_created ASC LIMIT 1";
            String getLatestMessageQuery =
                    "SELECT * FROM " +
                    "(SELECT * FROM messages_spam WHERE user_id = ? ORDER BY time_created DESC LIMIT " + BotConfig.ANTI_SPAM_WORDS_AMOUNT + ") AS recent_messages " +
                    "LIMIT 1";

            String updateAmountStrikesQuery = "UPDATE strikes_spam SET amount = ?, most_recent_given = ? WHERE discord_user_id = ?";
            String updateAmountStrikesToZeroQuery = "UPDATE strikes_spam SET amount = 0 WHERE discord_user_id = ?";

            PreparedStatement insertUsrStmt = conn.prepareStatement(insertUserQuery);
            PreparedStatement insertStrikesStmt = conn.prepareStatement(insertStrikesQuery);

            PreparedStatement insertMsgStmt = conn.prepareStatement(insertMessageQuery);
            PreparedStatement getFirstMsgStmt = conn.prepareStatement(getFirstMessageQuery);
            PreparedStatement getLastMsgStmt = conn.prepareStatement(getLatestMessageQuery);
            PreparedStatement getAmountStrikesStmt = conn.prepareStatement(getAmountStrikesQuery);

            PreparedStatement updateAmountStrikes = conn.prepareStatement(updateAmountStrikesQuery);
            PreparedStatement updateAmountStrikesToZero = conn.prepareStatement(updateAmountStrikesToZeroQuery);

            insertUsrStmt.setLong(1, discordId);

            insertStrikesStmt.setLong(1, discordId);
            insertStrikesStmt.setTimestamp(2, new Timestamp(msgCreated));

            insertMsgStmt.setLong(1, discordMsgId);
            insertMsgStmt.setLong(2, discordId);
            insertMsgStmt.setTimestamp(3, new Timestamp(msgCreated));

            getFirstMsgStmt.setLong(1, discordId);

            getLastMsgStmt.setLong(1, discordId);

            getAmountStrikesStmt.setLong(1, discordId);

            insertUsrStmt.executeUpdate();
            insertStrikesStmt.executeUpdate();
            insertMsgStmt.executeUpdate();

            ResultSet resultFirstMsg = getFirstMsgStmt.executeQuery();
            ResultSet resultLastMsg = getLastMsgStmt.executeQuery();
            ResultSet resultAmountStrikes = getAmountStrikesStmt.executeQuery();
            var strikes = 0;

            while (resultFirstMsg.next()) {
                timeFirstMsgCreated = resultFirstMsg.getTimestamp("time_created");
            }

            while (resultLastMsg.next()) {
                timeLastMsgCreated = resultLastMsg.getTimestamp("time_created");
            }

            Timestamp lastTimeStrikeGiven = null;
            while (resultAmountStrikes.next()){
                strikes = resultAmountStrikes.getInt("amount");
                lastTimeStrikeGiven = resultAmountStrikes.getTimestamp("most_recent_given");
            }

            var timeFirstMsgCreatedSeconds = timeFirstMsgCreated.toInstant().getEpochSecond();
            var timeLastMsgCreatedSeconds = timeLastMsgCreated.toInstant().getEpochSecond();

            // if the time the most recent msg was created minus the first one is smaller than what the threshhold is
            // mute them for spamming
            if (timeLastMsgCreatedSeconds - timeFirstMsgCreatedSeconds <= ANTI_SPAM_TIME_AMOUNT && timeLastMsgCreatedSeconds != timeFirstMsgCreatedSeconds){

                // to prevent the bot spam muting: check that the difference between the time now and
                // the last time a strike was given is at least greater than 5 secs
                // we could make this (5 secs diff) an env var but i dont see the point atm.
                if (new Timestamp(msgCreated).toInstant().getEpochSecond() - lastTimeStrikeGiven.toInstant().getEpochSecond() > 5) {
                    strikes++;

                    updateAmountStrikes.setInt(1, strikes);
                    updateAmountStrikes.setTimestamp(2, new Timestamp(msgCreated));
                    updateAmountStrikes.setLong(3, discordId);
                    updateAmountStrikes.executeUpdate();
                    updateAmountStrikes.close();

                    EmbedBuilder embed = new EmbedBuilder();

                    embed.setTitle("Someone got muted for spamming");
                    embed.addField("User", user.getAsMention(), true);
                    embed.addField("Minutes", String.valueOf(ANTI_SPAM_MUTE_AMOUNT), true);

                    embed.setColor(0xbb0a1e);
                    embed.setTimestamp(Instant.now());

                    // if strikes below or equal to allowed strikes send warning msg and give strike other wise
                    // ban them and set strikes to 0 for future (if they ever get unbanned)
                    if (strikes <= ANTI_SPAM_STRIKE_AMOUNT) {
                        int strikesCopy = strikes; //need to make this to be able to use in lambda below (intellij) said
                        event.getGuild()
                                .timeoutFor(usrSnowflake, Duration.ofMinutes(ANTI_SPAM_MUTE_AMOUNT))
                                .reason("Spamming")
                                .queue(success -> {
                                    event.getGuild().getChannelById(TextChannel.class , BotConfig.DARWIN_CHANNEL_ID)
                                            .sendMessageEmbeds(embed.build())
                                            .and(event.getMessage().reply("Stop spamming strike: " + strikesCopy + "/" + ANTI_SPAM_STRIKE_AMOUNT + " 3 strikes = ban."))
                                            .queue();
                                });
                    } else {

                        EmbedBuilder embedForBan = new EmbedBuilder();

                        embedForBan.setTitle("Someone got banned");
                        embedForBan.addField("User", user.getAsMention(), true);
                        embedForBan.addField("Reason", "Spamming", true);
                        embedForBan.setColor(0xbb0a1e);
                        embedForBan.setTimestamp(Instant.now());

                        event.getGuild()
                                .ban(usrSnowflake, 0, TimeUnit.MINUTES)
                                .reason("Spamming")
                                .queue(success -> {
                                            event.getGuild().getChannelById(TextChannel.class ,BotConfig.DARWIN_CHANNEL_ID)
                                                    .sendMessageEmbeds(embedForBan.build())
                                                    .queue();
                                        });

                        //alternatively we could delete the entry from the table, ill keep this in mind for later
                        //but keep it to 0 for simplicity for now
                        updateAmountStrikesToZero.setLong(1, discordId);
                        updateAmountStrikesToZero.executeUpdate();
                        updateAmountStrikesToZero.close();
                    }
                }
            }

            insertUsrStmt.close();
            insertStrikesStmt.close();
            insertMsgStmt.close();
            getFirstMsgStmt.close();
            getLastMsgStmt.close();
            conn.close();

            log.debug("[onMessageReceived] Closed all statements and connection.");

        } catch (SQLException ex) {
            log.error("SQLException: {}", ex.getMessage());
            log.error("SQLState: {}", ex.getSQLState());
            log.error("VendorError: {}", ex.getErrorCode());
        }
    }
}
