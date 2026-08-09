package va.rembot.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.BotConfig;
import va.rembot.database.dao.MessageDao;
import va.rembot.database.dao.StrikeSpamDao;
import va.rembot.database.dao.UserDao;
import va.rembot.database.models.DiscordUser;
import va.rembot.database.models.DiscordMessage;
import va.rembot.database.models.StrikeSpam;
import va.rembot.exceptions.MessageSpamNotFoundException;
import va.rembot.exceptions.StrikeSpamNotFoundException;
import va.rembot.lib.ModerationLib;

import java.sql.*;

@Slf4j
public class AntiSpamFilter extends ListenerAdapter {

    private static final int ANTI_SPAM_TIME_AMOUNT = BotConfig.getAntiSpamTimeAmountInt();
    private static final int ANTI_SPAM_STRIKE_AMOUNT = BotConfig.getAntiSpamStrikeAmountInt();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (ModerationLib.isMod(event.getMember())) return;

        UserDao userDao = new UserDao();
        StrikeSpamDao strikeSpamDao = new StrikeSpamDao();
        MessageDao messageDao = new MessageDao();

        Message msg = event.getMessage();
        long msgCreated = msg.getTimeCreated().toInstant().toEpochMilli();
        long discordId = event.getMember().getIdLong();
        long discordMsgId = event.getMessageIdLong();
        UserSnowflake usrSnowflake = UserSnowflake.fromId(discordId);
        Member user = event.getMember();
        Timestamp timeFirstMsgCreated;
        Timestamp timeLastMsgCreated;
        String msgContent = msg.getContentRaw();
        StringBuilder attachmentLinks = new StringBuilder();

        for (Attachment file : msg.getAttachments())
            attachmentLinks.append(file.getUrl()).append(" ");

        userDao.create(new DiscordUser(discordId));
        messageDao.create(new DiscordMessage(discordMsgId, discordId, new Timestamp(msgCreated), msgContent, attachmentLinks.toString()));
        strikeSpamDao.create(new StrikeSpam(discordId, 0, new Timestamp(msgCreated)));

        timeFirstMsgCreated = messageDao
                .getFirst(discordId)
                .orElseThrow(() -> new MessageSpamNotFoundException("messageDao could not find first entry for user with discord id: " + discordId)).timeCreated();
        timeLastMsgCreated = messageDao
                .getLatest(discordId)
                .orElseThrow(() -> new MessageSpamNotFoundException("messageDao could not find latest entry for user with discord id: " + discordId)).timeCreated();

        int strikes;
        Timestamp lastTimeStrikeGiven;

        strikes = strikeSpamDao
                .getAmount(discordId)
                .orElseThrow(() -> new StrikeSpamNotFoundException("strikeSpamDao could not find amount for user with discord id: " + discordId)).amount();
        lastTimeStrikeGiven = strikeSpamDao
                .getAmount(discordId)
                .orElseThrow(() -> new StrikeSpamNotFoundException("strikeSpamDao could not find lastTimeStrikeGiven for user with discord id: " + discordId)).mostRecentStrike();

        long timeFirstMsgCreatedSeconds = timeFirstMsgCreated.toInstant().getEpochSecond();
        long timeLastMsgCreatedSeconds = timeLastMsgCreated.toInstant().getEpochSecond();
        long timeLastTimeStrikeGivenSeconds = lastTimeStrikeGiven.toInstant().getEpochSecond();
        long timeMsgSentNowSeconds = new Timestamp(msgCreated).toInstant().getEpochSecond();

        if (timeLastMsgCreatedSeconds - timeFirstMsgCreatedSeconds <= ANTI_SPAM_TIME_AMOUNT && timeLastMsgCreatedSeconds != timeFirstMsgCreatedSeconds) {

            if (timeMsgSentNowSeconds - timeLastTimeStrikeGivenSeconds > 5) {
                strikes++;
                strikeSpamDao.update(new StrikeSpam(discordId, strikes, new Timestamp(msgCreated)));

                if (strikes < ANTI_SPAM_STRIKE_AMOUNT)
                    ModerationLib.muteSpam(event, usrSnowflake, "Spamming", strikes, user.getUser());
                else {
                    ModerationLib.banGeneric(event, usrSnowflake, "Spamming", user.getUser());
                    strikeSpamDao.update(new StrikeSpam(discordId, 0, new Timestamp(msgCreated)));
                }
            }
        }
    }
}
