package va.rembot.lib;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
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
public class ModerationLib {

    private static final int EMBED_MESSAGE_COLOR = 0xbb0a1e;
    private static final String EMBED_MESSAGE_TITLE_BANNED = "Someone got banned";
    private static final String EMBED_MESSAGE_TITLE_MUTED = "Someone got muted";

    public static boolean isMod(Member member) {

        Role modRole = member.getJDA().getRoleById(BotConfig.getModRoleIdLong());
        return member.getUnsortedRoles().contains(modRole);
    }

    public static void banUsingSlashCommand(SlashCommandInteractionEvent event, UserSnowflake usrSnowflake, String reason, User slashCommandUser, User targetUser) {

        MessageEmbed embed = buildEmbedForBan(targetUser, reason, slashCommandUser);

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
                            logError("banUsingSlashCommand",
                                    "Bot doesn't have enough permissions to ban the target user. (Bot probably has a lower or same discord role hierarchy as the target).",
                                    "ban",
                                    slashCommandUser,
                                    targetUser);
                            event.getHook()
                                    .editOriginal("Failed to ban that user because I don't have sufficient perms (most likely need a role with higher permissions than the target)." + slashCommandUser.getAsMention())
                                    .queue();
                        })
                        .handle(ErrorResponse.UNKNOWN_USER, e -> {
                            logError("banUsingSlashCommand",
                                    "Dont know who the target user is (Unknown user).",
                                    "ban",
                                    slashCommandUser,
                                    targetUser);
                            event.getHook()
                                    .editOriginal("I can't find the person you are trying to ban (unknown user)." + slashCommandUser.getAsMention())
                                    .queue();
                        }));
    }

    public static void banGeneric(MessageReceivedEvent event, UserSnowflake usrSnowflake, String reason, User targetUser) {

        MessageEmbed embed = buildEmbedForBanGeneric(targetUser, reason);

        event.getGuild()
                .ban(usrSnowflake, 0, TimeUnit.MINUTES)
                .reason(reason)
                .queue(success -> {
                    event.getGuild().getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID)
                            .sendMessageEmbeds(embed)
                            .queue();
                }, new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                            logError("banGeneric",
                                    "Bot doesn't have enough permissions to ban the target user. (Bot probably has a lower or same discord role hierarchy as the target).");
                        })
                        .handle(ErrorResponse.UNKNOWN_USER, e -> {
                            logError("banGeneric",
                                    "Dont know who the target user is (Unknown user).");
                        }));
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
                            logError("kickUsingSlashCommand",
                                    "Bot doesn't have enough permissions to kick the target user. (Bot probably has a lower or same discord role hierarchy as the target).",
                                    "kick",
                                    slashCommandUser,
                                    targetUser);
                            event.getHook()
                                    .editOriginal("Failed to kick that user because I don't have sufficient perms (most likely need a role with higher permissions than the target)." + slashCommandUser.getAsMention())
                                    .queue();
                        })
                        .handle(ErrorResponse.UNKNOWN_MEMBER, e -> {
                            logError("kickUsingSlashCommand",
                                    "The member that was being kicked was already removed from this server before finishing the kicking task.",
                                    "kick",
                                    slashCommandUser,
                                    targetUser);
                            event.getHook()
                                    .editOriginal("The user you tried to kick was already removed from this server." + slashCommandUser.getAsMention())
                                    .queue();
                        }));
    }

    public static void muteUsingSlashCommand(SlashCommandInteractionEvent event, UserSnowflake usrSnowflake, String reason, int muteTimeTotalMinutes, User slashCommandUser, User targetUser) {

        MessageEmbed embed = buildEmbedForMute(targetUser, reason, slashCommandUser, muteTimeTotalMinutes);

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
                            logError("muteUsingSlashCommand",
                                    "Bot doesn't have enough permissions to mute the target user. (Bot probably has a lower or same discord role hierarchy as the target).",
                                    "mute",
                                    slashCommandUser,
                                    targetUser);
                            event.getHook()
                                    .editOriginal("Failed to muted that user because I don't have sufficient perms (most likely need a role with higher permissions than the target)." + slashCommandUser.getAsMention())
                                    .queue();
                        })
                        .handle(ErrorResponse.UNKNOWN_MEMBER, e -> {
                            logError("muteUsingSlashCommand",
                                    "The member that was being muted was already removed from this server before finishing the muting task.",
                                    "mute",
                                    slashCommandUser,
                                    targetUser);
                            event.getHook()
                                    .editOriginal("The user you tried to mute was already removed from this server." + slashCommandUser.getAsMention())
                                    .queue();
                        }));
    }

    public static void muteSpam(MessageReceivedEvent event, UserSnowflake usrSnowflake, String reason, int strikes, User targetUsr) {

        MessageEmbed embed = buildEmbedForMuteSpam(targetUsr, BotConfig.getAntiSpamMuteAmountInt());

        event.getGuild()
                .timeoutFor(usrSnowflake, Duration.ofMinutes(BotConfig.getAntiSpamMuteAmountInt()))
                .reason(reason)
                .queue(success -> {
                    event.getGuild().getChannelById(TextChannel.class, BotConfig.DARWIN_CHANNEL_ID)
                            .sendMessageEmbeds(embed)
                            .and(event.getMessage().reply("Stop spamming strike: " + strikes + "/" + BotConfig.getAntiSpamStrikeAmountInt() + " 3 strikes = ban."))
                            .queue();
                }, new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                            logError("muteSpam",
                                    "Bot doesn't have enough permissions to mute the target user. (Bot probably has a lower or same discord role hierarchy as the target).");
                        })
                        .handle(ErrorResponse.UNKNOWN_MEMBER, e -> {
                            logError("muteSpam",
                                    "The member that was being muted was already removed from this server before finishing the muting task.");
                        }));
    }

    public static void unbanUsingSlashCommand(SlashCommandInteractionEvent event, UserSnowflake usrSnowflake, String reason, User slashCommandUser, User targetUser) {

        event.getGuild()
                .unban(usrSnowflake)
                .reason(reason)
                .queue(success -> {
                    event.getGuild().getChannelById(TextChannel.class , BotConfig.LOG_CHANNEL_ID)
                            .sendMessage("**[USER UNBAN]**: " + usrSnowflake.getAsMention() + " <R:" + reason + "> [MOD:" + slashCommandUser.getAsMention() + "]")
                            .and(event.getHook().deleteOriginal())
                            .queue();
                }, new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                            logError("unbanUsingSlashCommand",
                                    "Bot doesn't have enough permissions to unban the target user. (Bot probably has a lower or same discord role hierarchy as the target).",
                                    "unban",
                                    slashCommandUser,
                                    targetUser);
                            event.getHook()
                                    .editOriginal("Failed to unban that user because I don't have sufficient perms (most likely need a role with higher permissions than the target)." + slashCommandUser.getAsMention())
                                    .queue();
                        })
                        .handle(ErrorResponse.UNKNOWN_BAN, e -> {
                            logError("unbanUsingSlashCommand",
                                    "Couldn't unban that user because the ban is unknown (possibly not banned?).",
                                    "unban",
                                    slashCommandUser,
                                    targetUser);
                            event.getHook()
                                    .editOriginal("Failed to unban that user because I think that user is not banned (the ban is unknown to me)." + slashCommandUser.getAsMention())
                                    .queue();
                        })
                        .handle(ErrorResponse.UNKNOWN_USER, e -> {
                            logError("unbanUsingSlashCommand",
                                    "Dont know who the target user is (Unknown user).",
                                    "unban",
                                    slashCommandUser,
                                    targetUser);
                            event.getHook()
                                    .editOriginal("I can't find the person you are trying to unban (unknown user)." + slashCommandUser.getAsMention())
                                    .queue();
                        }));
    }

    public static void unmuteUsingSlashCommand(SlashCommandInteractionEvent event, UserSnowflake usrSnowflake, String reason, User slashCommandUser, User targetUser) {

        event.getGuild()
                .getMemberById(targetUser.getId())
                .removeTimeout()
                .queue(success -> {
                    event.getGuild().getChannelById(TextChannel.class, BotConfig.LOG_CHANNEL_ID)
                            .sendMessage("**[USER UNMUTE]**: " + targetUser.getAsMention() + " <R:" + reason + "> [MOD:" + slashCommandUser.getAsMention() + "]")
                            .and(event.getHook().deleteOriginal())
                            .queue();
                }, new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                            logError("unmuteUsingSlashCommand",
                                    "Bot doesn't have enough permissions to unmute the target user. (Bot probably has a lower or same discord role hierarchy as the target).",
                                    "unmute",
                                    slashCommandUser,
                                    targetUser);
                            event.getHook()
                                    .editOriginal("Failed to unmute that user because I don't have sufficient perms (most likely need a role with higher permissions than the target)." + slashCommandUser.getAsMention())
                                    .queue();
                        })
                        .handle(ErrorResponse.UNKNOWN_MEMBER, e -> {
                            logError("unmuteUsingSlashCommand",
                                    "The target user was removed from the guild before bot could unmute them.",
                                    "unmute",
                                    slashCommandUser,
                                    targetUser);
                            event.getHook()
                                    .editOriginal("Failed to unmute that user because they were removed from the guild before the unmuting task finished." + slashCommandUser.getAsMention())
                                    .queue();
                        }));
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

    private static void logError(String methodName, String error) {
        log.error("[{}] {}", methodName, error);
    }

    private static void logError(String methodName, String error, String commandBeingUsed, User slashCommandUser, User targetUser) {
        log.error("[{}] {}", methodName, error);
        log.error("[{}] {} tried to {} {}", methodName, slashCommandUser, commandBeingUsed, targetUser);
    }
}
