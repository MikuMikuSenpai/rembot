package va.rembot.commands.slash.admin;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.lib.ModerationLib;

@Slf4j
public class Ban extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("ban")){

            event.deferReply(true).queue();

            var target = event.getOption("username").getAsUser();
            var usrSnowflake = UserSnowflake.fromId(target.getId());
            var slashCommandUser = event.getInteraction().getUser();

            // "reason" is an optional input, could be null so handle it:
            try {
                var reason = event.getOption("reason").getAsString();
                ModerationLib.banUsingSlashCommand(event, usrSnowflake, reason, slashCommandUser, target);
            } catch (NullPointerException e) {
                var reason = "No reason provided.";
                ModerationLib.banUsingSlashCommand(event, usrSnowflake, reason, slashCommandUser, target);
            }
        }
    }
}
