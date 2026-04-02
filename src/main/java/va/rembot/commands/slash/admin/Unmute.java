package va.rembot.commands.slash.admin;

import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.lib.ModerationLib;

public class Unmute extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("unmute")) {

            event.deferReply(true).queue();

            var target = event.getOption("username").getAsUser();

            if (!event.getGuild().getMemberById(target.getId()).isTimedOut()) { //if user != timedout dont unmute
                event.getHook().editOriginal("This user is not muted, can't unmute them.").queue();
                return;
            }

            var reason = "No reason provided";
            var usrSnowflake = UserSnowflake.fromId(target.getId());
            var slashCommandUser = event.getUser();

            try {
                reason = event.getOption("reason").getAsString();
            } catch (NullPointerException ignored) {}

            ModerationLib.unmuteUsingSlashCommand(event, usrSnowflake, reason, slashCommandUser, target);

        }
    }
}
