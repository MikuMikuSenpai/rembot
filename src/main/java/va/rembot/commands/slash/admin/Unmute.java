package va.rembot.commands.slash.admin;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.lib.ModerationLib;

public class Unmute extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("unmute")) {

            event.deferReply(true).queue();

            User target = event.getOption("username").getAsUser();

            if (!event.getGuild().getMemberById(target.getId()).isTimedOut()) {
                event.getHook().editOriginal("This user is not muted, can't unmute them.").queue();
                return;
            }

            String reason;
            UserSnowflake usrSnowflake = UserSnowflake.fromId(target.getId());
            User slashCommandUser = event.getUser();

            try {
                reason = event.getOption("reason").getAsString();
            } catch (NullPointerException e) {
                reason = "No reason provided.";
            }

            ModerationLib.unmuteUsingSlashCommand(event, usrSnowflake, reason, slashCommandUser, target);

        }
    }
}
