package va.rembot.commands.slash.admin;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import va.rembot.BotConfig;

import java.time.Duration;
import java.time.Instant;

@Slf4j
public class Mute extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("mute")) {

            event.deferReply(true).queue();

            var reason = "No reason provided";
            var target = event.getOption("username").getAsUser();
            var minutes = event.getOption("minutes").getAsInt();
            var usrSnowflake = UserSnowflake.fromId(target.getId());
            var slashCommandUser = event.getInteraction().getUser();

            // "reason" is an optional input, could be null so handle it:
            try {
                reason = event.getOption("reason").getAsString();
            } catch (NullPointerException ignored) {}

            // "hours" is an optional input, could be null so handle it:
            try {
                var hours = event.getOption("hours").getAsInt();
                var hoursToMinutes = hours * 60;
                var totalMuteTime = minutes + hoursToMinutes;
                var embedMsg = buildEmbed(target, reason, slashCommandUser, totalMuteTime);
                mute(event, usrSnowflake, reason, totalMuteTime, slashCommandUser, target, embedMsg);
            } catch (NullPointerException e) {
                var embedMsg = buildEmbed(target, reason, slashCommandUser, minutes);
                mute(event, usrSnowflake, reason, minutes, slashCommandUser, target, embedMsg);
            }
        }
    }

    private MessageEmbed buildEmbed(User targetUser, String reason, User moderatorUser, int muteTimeMinutes){
        EmbedBuilder embed = new EmbedBuilder();

        embed.setTitle("Someone got muted");
        embed.addField("User", targetUser.getAsMention(), true);
        embed.addField("Mod", moderatorUser.getAsMention(), true);
        embed.addField("Minutes", String.valueOf(muteTimeMinutes), true);
        embed.addField("Reason", reason, false);

        embed.setColor(0xbb0a1e);
        embed.setTimestamp(Instant.now());

        return embed.build();
    }

    private void mute(SlashCommandInteractionEvent event, UserSnowflake usrSnowflake, String reason, int muteTimeTotalMinutes, User slashCommandUser, User targetUser, MessageEmbed embed) {
        event.getGuild()
                .timeoutFor(usrSnowflake, Duration.ofMinutes(muteTimeTotalMinutes))
                .reason(reason)
                .queue(success -> {
                    event.getGuild().getChannelById(TextChannel.class , BotConfig.DARWIN_CHANNEL_ID)
                            .sendMessageEmbeds(embed)
                            .and(event.getHook().deleteOriginal())
                            .queue();
                }, new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                            log.error("Bot doesn't have enough permissions to mute the target user. (Bot probably has a lower or same discord role hierarchy as the target).");
                            log.error("{} tried to mute {}", slashCommandUser, targetUser);
                            event.getHook()
                                    .editOriginal("Failed to muted that user because I don't have sufficient perms (most likely need a role with higher permissions than the target)." + slashCommandUser.getAsMention())
                                    .queue();
                        })
                        .handle(ErrorResponse.UNKNOWN_MEMBER, e -> {
                            log.error("The member that was being muted was already removed from this server before finishing the muting task.");
                            log.error("{} tried to muted {}", slashCommandUser, targetUser);
                            event.getHook()
                                    .editOriginal("The user you tried to mute was already removed from this server." + slashCommandUser.getAsMention())
                                    .queue();
                        }));
    }
}
