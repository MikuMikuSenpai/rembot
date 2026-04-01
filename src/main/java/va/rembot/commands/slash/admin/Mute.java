package va.rembot.commands.slash.admin;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.lib.ModerationLib;

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
                ModerationLib.muteUsingSlashCommand(event, usrSnowflake, reason, totalMuteTime, slashCommandUser, target);
            } catch (NullPointerException e) {
                ModerationLib.muteUsingSlashCommand(event, usrSnowflake, reason, minutes, slashCommandUser, target);
            }
        }
    }
}
