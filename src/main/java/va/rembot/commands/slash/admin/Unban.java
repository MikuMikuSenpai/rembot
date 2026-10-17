package va.rembot.commands.slash.admin;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.lib.ModerationLib;

public class Unban extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("unban")) {

            event.deferReply(true).queue();

            User target = event.getOption("username").getAsUser();
            UserSnowflake usrSnowflake = UserSnowflake.fromId(target.getId());
            User slashCommandUser = event.getUser();
            String reason;

            try {
                reason = event.getOption("reason").getAsString();
            } catch (NullPointerException e) {
                reason = "No reason provided.";
            }

            ModerationLib.unbanUsingSlashCommand(event, usrSnowflake, reason, slashCommandUser, target);
        }
    }
}
