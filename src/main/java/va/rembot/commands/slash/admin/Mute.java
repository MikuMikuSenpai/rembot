package va.rembot.commands.slash.admin;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import va.rembot.BotConfig;

import java.time.Duration;

@Slf4j
public class Mute extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("mute")) {

            event.deferReply(true).queue();

            String reason = "No reason provided";
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
                mute(event, usrSnowflake, reason, totalMuteTime, slashCommandUser, target);
            } catch (NullPointerException e) {
                mute(event, usrSnowflake, reason, minutes, slashCommandUser, target);
            }
        }
    }

    private void mute(SlashCommandInteractionEvent event, UserSnowflake usrSnowflake, String reason, int muteTimeTotalMinutes, User slashCommandUser, User targetUser) {
        event.getGuild()
                .timeoutFor(usrSnowflake, Duration.ofMinutes(muteTimeTotalMinutes))
                .reason(reason)
                .queue(success -> {
                    //TODO add your frontend logic here
                    event.getGuild().getChannelById(TextChannel.class , BotConfig.DARWIN_CHANNEL_ID)
                            .sendMessage("[DARWIN CHANNEL] Some dumbass got muted lulz heres his name: " + usrSnowflake.getAsMention() + " and the reason: " + reason)
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
                                    .editOriginal("The user you tried to mute was already removed from this server. (failure: this should be rare)" + slashCommandUser.getAsMention())
                                    .queue();
                        }));
    }
}
