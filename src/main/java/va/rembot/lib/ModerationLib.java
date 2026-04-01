package va.rembot.lib;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import va.rembot.BotConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
/// Central class for accessing moderation methods, also to customize them (for example the embed messages).
public class ModerationLib {

    private static final int EMBED_MESSAGE_COLOR = 0xbb0a1e;
    private static final String EMBED_MESSAGE_TITLE_BANNED = "Someone got banned";
    private static final String EMBED_MESSAGE_TITLE_MUTED = "Someone got muted";

    public static void banUsingSlashCommand(SlashCommandInteractionEvent event, UserSnowflake usrSnowflake, String reason, User slashCommandUser, User targetUser) {

        var embed = buildEmbedForBan(targetUser, reason, slashCommandUser);

        event.getGuild()
                .ban(usrSnowflake, 0, TimeUnit.MINUTES)
                .reason(reason)
                .queue(success -> {
                    event.getGuild().getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID)
                            .sendMessageEmbeds(embed)
                            .and(event.getHook().deleteOriginal())
                            .queue();
                }, new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                            logBanErrorSlashCommand("Bot doesn't have enough permissions to ban the target user. (Bot probably has a lower or same discord role hierarchy as the target).", slashCommandUser, targetUser);
                            event.getHook()
                                    .editOriginal("Failed to ban that user because I don't have sufficient perms (most likely need a role with higher permissions than the target)." + slashCommandUser.getAsMention())
                                    .queue();
                        })
                        .handle(ErrorResponse.UNKNOWN_USER, e -> {
                            logBanErrorSlashCommand("Dont know who the target user is (Unknown user).", slashCommandUser, targetUser);
                            event.getHook()
                                    .editOriginal("I can't find the person you are trying to ban (unknown user)." + slashCommandUser.getAsMention())
                                    .queue();
                        }));
    }

    public static void banGeneric(MessageReceivedEvent event, UserSnowflake usrSnowflake, String reason, User targetUser) {

        var embed = buildEmbedForBanGeneric(targetUser, reason);

        event.getGuild()
                .ban(usrSnowflake, 0, TimeUnit.MINUTES)
                .reason(reason)
                .queue(success -> {
                    event.getGuild().getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID)
                            .sendMessageEmbeds(embed)
                            .queue(null, new ErrorHandler()
                                    .handle(ErrorResponse.UNKNOWN_CHANNEL, e -> {
                                        logBanErrorGeneric("Failed to find darwin channel, it was deleted.");
                                    }));
                });
    }

    public static void kickUsingSlashCommand(SlashCommandInteractionEvent event, UserSnowflake usrSnowflake, String reason, User slashCommandUser, User targetUser) {

        event.getGuild()
                .kick(usrSnowflake)
                .reason(reason)
                .queue(success -> {
                    event.getGuild().getChannelById(TextChannel.class , BotConfig.LOG_CHANNEL_ID)
                            .sendMessage("**[USER KICK]**: " + usrSnowflake.getAsMention() + " <R:" + reason + "> [MOD:" + slashCommandUser.getAsMention() + "]")
                            .and(event.getHook().deleteOriginal())
                            .queue();
                }, new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                            log.error("Bot doesn't have enough permissions to kick the target user. (Bot probably has a lower or same discord role hierarchy as the target).");
                            log.error("{} tried to kick {}", slashCommandUser, targetUser);
                            event.getHook()
                                    .editOriginal("Failed to kick that user because I don't have sufficient perms (most likely need a role with higher permissions than the target)." + slashCommandUser.getAsMention())
                                    .queue();
                        })
                        .handle(ErrorResponse.UNKNOWN_MEMBER, e -> {
                            log.error("The member that was being kicked was already removed from this server before finishing the kicking task.");
                            log.error("{} tried to kick {}", slashCommandUser, targetUser);
                            event.getHook()
                                    .editOriginal("The user you tried to kick was already removed from this server." + slashCommandUser.getAsMention())
                                    .queue();
                        }));
    }

    public static void muteUsingSlashCommand(SlashCommandInteractionEvent event, UserSnowflake usrSnowflake, String reason, int muteTimeTotalMinutes, User slashCommandUser, User targetUser) {

        var embed = buildEmbedForMute(targetUser, reason, slashCommandUser, muteTimeTotalMinutes);

        event.getGuild()
                .timeoutFor(usrSnowflake, Duration.ofMinutes(muteTimeTotalMinutes))
                .reason(reason)
                .queue(success -> {
                    event.getGuild().getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID)
                            .sendMessageEmbeds(embed)
                            .and(event.getHook().deleteOriginal())
                            .queue();
                }, new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                            logMuteErrorSlashCommand("Bot doesn't have enough permissions to mute the target user. (Bot probably has a lower or same discord role hierarchy as the target).", slashCommandUser, targetUser);
                            event.getHook()
                                    .editOriginal("Failed to muted that user because I don't have sufficient perms (most likely need a role with higher permissions than the target)." + slashCommandUser.getAsMention())
                                    .queue();
                        })
                        .handle(ErrorResponse.UNKNOWN_MEMBER, e -> {
                            logMuteErrorSlashCommand("The member that was being muted was already removed from this server before finishing the muting task.", slashCommandUser, targetUser);
                            event.getHook()
                                    .editOriginal("The user you tried to mute was already removed from this server." + slashCommandUser.getAsMention())
                                    .queue();
                        }));
    }

    public static void muteSpam(MessageReceivedEvent event, UserSnowflake usrSnowflake, String reason, int strikes, User targetUsr) {

        var embed = buildEmbedForMuteSpam(targetUsr, BotConfig.getAntiSpamMuteAmountInt());

        event.getGuild()
                .timeoutFor(usrSnowflake, Duration.ofMinutes(BotConfig.getAntiSpamMuteAmountInt()))
                .reason(reason)
                .queue(success -> {
                    event.getGuild().getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID)
                            .sendMessageEmbeds(embed)
                            .and(event.getMessage().reply("Stop spamming strike: " + strikes + "/" + BotConfig.getAntiSpamStrikeAmountInt() + " 3 strikes = ban."))
                            .queue();
                });
    }

    private static MessageEmbed buildEmbedForBan(User targetUser, String reason, User moderatorUser){
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(EMBED_MESSAGE_TITLE_BANNED);
        embed.addField("User", targetUser.getAsMention(), true);
        embed.addField("Mod", moderatorUser.getAsMention(), true);
        embed.addField("Reason", reason, false);
        embed.setColor(EMBED_MESSAGE_COLOR);
        embed.setTimestamp(Instant.now());

        return embed.build();
    }

    private static MessageEmbed buildEmbedForBanGeneric(User targetUser, String reason){
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(EMBED_MESSAGE_TITLE_BANNED);
        embed.addField("User", targetUser.getAsMention(), true);
        embed.addField("Reason", reason, true);
        embed.setColor(EMBED_MESSAGE_COLOR);
        embed.setTimestamp(Instant.now());

        return embed.build();
    }

    private static MessageEmbed buildEmbedForMute(User targetUser, String reason, User moderatorUser, int muteTimeMinutes){
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle(EMBED_MESSAGE_TITLE_MUTED);
        embed.addField("User", targetUser.getAsMention(), true);
        embed.addField("Mod", moderatorUser.getAsMention(), true);
        embed.addField("Minutes", String.valueOf(muteTimeMinutes), true);
        embed.addField("Reason", reason, false);

        embed.setColor(EMBED_MESSAGE_COLOR);
        embed.setTimestamp(Instant.now());

        return embed.build();
    }

    private static MessageEmbed buildEmbedForMuteSpam(User targetUser, int muteTimeMinutes){
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle("Someone got muted for spamming");
        embed.addField("User", targetUser.getAsMention(), true);
        embed.addField("Minutes", String.valueOf(muteTimeMinutes), true);

        embed.setColor(EMBED_MESSAGE_COLOR);
        embed.setTimestamp(Instant.now());

        return embed.build();
    }

    private static void logBanErrorSlashCommand(String error, User slashCommandUser, User targetUser) {
        log.error(error);
        log.error("{} tried to ban {}", slashCommandUser, targetUser);
    }

    private static void logBanErrorGeneric(String error) {
        log.error(error);
    }

    private static void logMuteErrorSlashCommand(String error, User slashCommandUser, User targetUser) {
        log.error(error);
        log.error("{} tried to mute {}", slashCommandUser, targetUser);
    }

}
