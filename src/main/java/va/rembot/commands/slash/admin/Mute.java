package va.rembot.commands.slash.admin;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.User;
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

            User target = event.getOption("username").getAsUser();
            int minutes = event.getOption("minutes").getAsInt();
            UserSnowflake usrSnowflake = UserSnowflake.fromId(target.getId());
            User slashCommandUser = event.getUser();
            String reason;

            try {
                reason = event.getOption("reason").getAsString();
            } catch (NullPointerException ignored) {
                reason = "No reason provided.";
            }

            try {
                int hours = event.getOption("hours").getAsInt();
                int hoursToMinutes = hours * 60;
                int totalMuteTime = minutes + hoursToMinutes;
                ModerationLib.muteUsingSlashCommand(event, usrSnowflake, reason, totalMuteTime, slashCommandUser, target);
            } catch (NullPointerException e) {
                ModerationLib.muteUsingSlashCommand(event, usrSnowflake, reason, minutes, slashCommandUser, target);
            }
        }
    }
}
