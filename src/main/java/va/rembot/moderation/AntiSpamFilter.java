package va.rembot.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.BotConfig;
import va.rembot.database.dao.MessageSpamDao;
import va.rembot.database.dao.StrikeSpamDao;
import va.rembot.database.dao.UserDao;
import va.rembot.database.models.MessageSpam;
import va.rembot.database.models.StrikeSpam;
import va.rembot.database.models.User;
import va.rembot.exceptions.MessageSpamNotFoundException;
import va.rembot.exceptions.StrikeSpamNotFoundException;

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

        var modRole = event.getJDA().getRoleById(BotConfig.getModRoleIdLong());
        if (event.getMember().getUnsortedRoles().contains(modRole)) return;

        var usrDao = new UserDao();
        var strikeSpamDao = new StrikeSpamDao();
        var messageSpamDao = new MessageSpamDao();

        var msgCreated = event.getMessage().getTimeCreated().toInstant().toEpochMilli();
        var user = event.getMember();
        var discordId = event.getMember().getIdLong();
        var usrSnowflake = UserSnowflake.fromId(discordId);
        var discordMsgId = event.getMessageIdLong();
        Timestamp timeFirstMsgCreated;
        Timestamp timeLastMsgCreated;

        usrDao.create(new User(discordId));
        strikeSpamDao.create(new StrikeSpam(discordId, 0, new Timestamp(msgCreated)));
        messageSpamDao.create(new MessageSpam(discordMsgId, discordId, new Timestamp(msgCreated)));

        timeFirstMsgCreated = messageSpamDao
                .getFirst(discordId)
                .orElseThrow(() -> new MessageSpamNotFoundException("messageSpamDao could not find first entry for user with discord id: " + discordId)).getTimeCreated();
        timeLastMsgCreated = messageSpamDao
                .getLatest(discordId)
                .orElseThrow(() -> new MessageSpamNotFoundException("messageSpamDao could not find latest entry for user with discord id: " + discordId)).getTimeCreated();

        var strikes = 0;

        Timestamp lastTimeStrikeGiven;

        strikes = strikeSpamDao
                .getAmount(discordId)
                .orElseThrow(() -> new StrikeSpamNotFoundException("strikeSpamDao could not find amount for user with discord id: " + discordId)).getAmount();
        lastTimeStrikeGiven = strikeSpamDao
                .getAmount(discordId)
                .orElseThrow(() -> new StrikeSpamNotFoundException("strikeSpamDao could not find lastTimeStrikeGiven for user with discord id: " + discordId)).getMostRecentStrike();

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

                strikeSpamDao.update(new StrikeSpam(discordId, strikes, new Timestamp(msgCreated)));

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
                    strikeSpamDao.update(new StrikeSpam(discordId, 0, new Timestamp(msgCreated)));//TODO make an actual updateToZeroAmount() reads better
                }
            }
        }
    }
}
