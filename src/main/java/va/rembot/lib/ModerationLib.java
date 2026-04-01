package va.rembot.lib;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.requests.ErrorResponse;
import va.rembot.BotConfig;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ModerationLib {

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
                            logBanError("Bot doesn't have enough permissions to ban the target user. (Bot probably has a lower or same discord role hierarchy as the target).", slashCommandUser, targetUser);
                            event.getHook()
                                    .editOriginal("Failed to ban that user because I don't have sufficient perms (most likely need a role with higher permissions than the target)." + slashCommandUser.getAsMention())
                                    .queue();
                        })
                        .handle(ErrorResponse.UNKNOWN_USER, e -> {
                            logBanError("Dont know who the target user is (Unknown user).", slashCommandUser, targetUser);
                            event.getHook()
                                    .editOriginal("I can't find the person you are trying to ban (unknown user)." + slashCommandUser.getAsMention())
                                    .queue();
                        }));
    }

    private static MessageEmbed buildEmbedForBan(User targetUser, String reason, User moderatorUser){
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle("Someone got banned");
        embed.addField("User", targetUser.getAsMention(), true);
        embed.addField("Mod", moderatorUser.getAsMention(), true);
        embed.addField("Reason", reason, false);
        embed.setColor(0xbb0a1e);
        embed.setTimestamp(Instant.now());

        return embed.build();
    }

    private static void logBanError(String error, User slashCommandUser, User targetUser) {
        log.error(error);
        log.error("{} tried to ban {}", slashCommandUser, targetUser);
    }

}
