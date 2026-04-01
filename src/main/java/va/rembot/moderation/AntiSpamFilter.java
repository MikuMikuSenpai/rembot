package va.rembot.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.BotConfig;
import va.rembot.database.dao.MessageDao;
import va.rembot.database.dao.StrikeSpamDao;
import va.rembot.database.dao.UserDao;
import va.rembot.database.models.Message;
import va.rembot.database.models.StrikeSpam;
import va.rembot.database.models.User;
import va.rembot.exceptions.MessageSpamNotFoundException;
import va.rembot.exceptions.StrikeSpamNotFoundException;
import va.rembot.lib.ModerationLib;

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
        var messageDao = new MessageDao();

        var msgCreated = event.getMessage().getTimeCreated().toInstant().toEpochMilli();
        var user = event.getMember();
        var discordId = event.getMember().getIdLong();
        var usrSnowflake = UserSnowflake.fromId(discordId);
        var discordMsgId = event.getMessageIdLong();
        Timestamp timeFirstMsgCreated;
        Timestamp timeLastMsgCreated;

        usrDao.create(new User(discordId));
        strikeSpamDao.create(new StrikeSpam(discordId, 0, new Timestamp(msgCreated)));
        messageDao.create(new Message(discordMsgId, discordId, new Timestamp(msgCreated)));

        timeFirstMsgCreated = messageDao
                .getFirst(discordId)
                .orElseThrow(() -> new MessageSpamNotFoundException("messageDao could not find first entry for user with discord id: " + discordId)).timeCreated();
        timeLastMsgCreated = messageDao
                .getLatest(discordId)
                .orElseThrow(() -> new MessageSpamNotFoundException("messageDao could not find latest entry for user with discord id: " + discordId)).timeCreated();

        var strikes = 0;

        Timestamp lastTimeStrikeGiven;

        strikes = strikeSpamDao
                .getAmount(discordId)
                .orElseThrow(() -> new StrikeSpamNotFoundException("strikeSpamDao could not find amount for user with discord id: " + discordId)).amount();
        lastTimeStrikeGiven = strikeSpamDao
                .getAmount(discordId)
                .orElseThrow(() -> new StrikeSpamNotFoundException("strikeSpamDao could not find lastTimeStrikeGiven for user with discord id: " + discordId)).mostRecentStrike();

        var timeFirstMsgCreatedSeconds = timeFirstMsgCreated.toInstant().getEpochSecond();
        var timeLastMsgCreatedSeconds = timeLastMsgCreated.toInstant().getEpochSecond();

        // If the time between the latest msg and the first msg of the bunch (set by the user, defaults to 5). Is less
        // than our threshold we count that as spam.
        if (timeLastMsgCreatedSeconds - timeFirstMsgCreatedSeconds <= ANTI_SPAM_TIME_AMOUNT && timeLastMsgCreatedSeconds != timeFirstMsgCreatedSeconds){

            // Prevent bot spam muting user by leaving at least 5 secs between now and last time strike was given.
            if (new Timestamp(msgCreated).toInstant().getEpochSecond() - lastTimeStrikeGiven.toInstant().getEpochSecond() > 5) {
                strikes++;

                strikeSpamDao.update(new StrikeSpam(discordId, strikes, new Timestamp(msgCreated)));

                EmbedBuilder embed = new EmbedBuilder();

                embed.setTitle("Someone got muted for spamming");
                embed.addField("User", user.getAsMention(), true);
                embed.addField("Minutes", String.valueOf(ANTI_SPAM_MUTE_AMOUNT), true);

                embed.setColor(0xbb0a1e);
                embed.setTimestamp(Instant.now());

                // If strikes are below/equal our accepted value give strike, else ban. Set strikes to 0 if they ever
                // get unbanned that this procedure would still work with them.
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

                    ModerationLib.banGeneric(event, usrSnowflake, "Spamming", user.getUser());
                    
                    // Set strikes to 0 after banning, could delete too but chose this route.
                    strikeSpamDao.updateAmountToZero(new StrikeSpam(discordId, 0, new Timestamp(msgCreated)));
                }
            }
        }
    }
}
