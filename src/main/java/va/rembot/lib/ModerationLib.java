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

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ModerationLib {

    private static final int EMBED_MESSAGE_COLOR = 0xbb0a1e;
    private static final String EMBED_MESSAGE_TITLE_BANNED = "Someone got banned";

    public static void banUsingSlashCommand(SlashCommandInteractionEvent event, UserSnowflake usrSnowflake, String reason, User slashCommandUser, User targetUser) {

        var embed = buildEmbedForBan(targetUser, reason, slashCommandUser);

        event.getGuild()
                .ban(usrSnowflake, 0, TimeUnit.MINUTES)
                .reason(reason)
                .queue(success -> {
                    event.getGuild().getChannelById(TextChannel.class , BotConfig.DARWIN_CHANNEL_ID)
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
                    event.getGuild().getChannelById(TextChannel.class ,BotConfig.DARWIN_CHANNEL_ID)
                            .sendMessageEmbeds(embed)
                            .queue(null, new ErrorHandler()
                                    .handle(ErrorResponse.UNKNOWN_CHANNEL, e -> {
                                        logBanErrorGeneric("Failed to find darwin channel, it was deleted.");
                                    }));
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

    private static void logBanErrorSlashCommand(String error, User slashCommandUser, User targetUser) {
        log.error(error);
        log.error("{} tried to ban {}", slashCommandUser, targetUser);
    }

    private static void logBanErrorGeneric(String error) {
        log.error(error);
    }

}
