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
               kick(event, usrSnowflake, reason, slashCommandUser, target);
           } catch (NullPointerException e) {
               kick(event, usrSnowflake, "No reason provided.", slashCommandUser, target);
           }
        }
    }

    private void kick(SlashCommandInteractionEvent event, UserSnowflake usrSnowflake, String reason, User slashCommandUser, User targetUser) {
       event.getGuild()
               .kick(usrSnowflake)
               .reason(reason)
               .queue(success -> {
                   //TODO add your frontend logic here
                   event.getGuild().getChannelById(TextChannel.class , BotConfig.DARWIN_CHANNEL_ID)
                           .sendMessage("[DARWIN CHANNEL] Some dumbass got kicked lulz heres his name: " + usrSnowflake.getAsMention() + " and the reason: " + reason)
                           .and(event.getHook().deleteOriginal())
                           .queue();
               }, new ErrorHandler()
                       .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                           log.error("Bot doesn't have enough permissions to kick the target user. (Bot probably has a lower or same discord role hierarchy as the target).");
                           log.error("{} tried to kick {}", slashCommandUser, targetUser);
                           event.getHook()
                                   .editOriginal("Failed to kick that user because I don't have sufficient perms (most likely need a role with higher permissions than the target)." + slashCommandUser.getAsMention())
                                   .queue();
                       })
                       .handle(ErrorResponse.UNKNOWN_MEMBER, e -> {
                           log.error("The member that was being kicked was already removed from this server before finishing the kicking task.");
                           log.error("{} tried to kick {}", slashCommandUser, targetUser);
                           event.getHook()
                                   .editOriginal("The user you tried to kick was already removed from this server." + slashCommandUser.getAsMention())
                                   .queue();
                       }));
    }
}
