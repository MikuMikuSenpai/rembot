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

import java.util.concurrent.TimeUnit;

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
                ban(event, usrSnowflake, reason, slashCommandUser, target);
            } catch (NullPointerException e) {
                ban(event, usrSnowflake, "No reason provided.", slashCommandUser, target);
            }
        }
    }

    private void ban(SlashCommandInteractionEvent event, UserSnowflake usrSnowflake, String reason, User slashCommandUser, User targetUser){
        event.getGuild()
                .ban(usrSnowflake, 0, TimeUnit.MINUTES)
                .reason(reason)
                .queue(success -> {
                    //TODO add your frontend logic here
                    event.getGuild().getChannelById(TextChannel.class ,BotConfig.DARWIN_CHANNEL_ID)
                            .sendMessage("[DARWIN CHANNEL] Some dumbass got banned lulz heres his name: " + usrSnowflake.getAsMention() + " and the reason: " + reason)
                            .and(event.getHook().deleteOriginal())
                            .queue();
                }, new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS,
                                e -> {
                                    log.error("Bot doesn't have enough permissions to ban the target user. (Bot probably has a lower or same discord role hierarchy as the target).");
                                    log.error("{} tried to ban {}", slashCommandUser, targetUser);
                                    event.getHook()
                                            .editOriginal("Failed to ban that user because I don't have sufficient perms (most likely need a role with higher permissions than the target)." + slashCommandUser.getAsMention())
                                            .queue();
                                })
                        .handle(ErrorResponse.UNKNOWN_USER,
                                e -> {
                                    log.error("Dont know who the target user is (Unknown user).");
                                    log.error("{} tried to ban {}", slashCommandUser, targetUser);
                                    event.getHook()
                                            .editOriginal("I can't find the person you are trying to ban (unknown user)." + slashCommandUser.getAsMention())
                                            .queue();
                                }));
    }
}
