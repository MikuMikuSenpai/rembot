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
import va.rembot.lib.ModerationLib;

@Slf4j
public class Kick extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("kick")){

           event.deferReply(true).queue();

           var target = event.getOption("username").getAsUser();
           var usrSnowflake = UserSnowflake.fromId(target.getId());
           var slashCommandUser = event.getInteraction().getUser();

           // "reason" is an optional input, could be null so handle it:
           try {
               var reason = event.getOption("reason").getAsString();
               ModerationLib.kickUsingSlashCommand(event, usrSnowflake, reason, slashCommandUser, target);
           } catch (NullPointerException e) {
               ModerationLib.kickUsingSlashCommand(event, usrSnowflake, "No reason provided.", slashCommandUser, target);
           }
        }
    }
}
