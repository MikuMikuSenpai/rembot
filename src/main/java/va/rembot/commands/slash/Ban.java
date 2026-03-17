package va.rembot.commands.slash;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.BotConfig;

import java.util.concurrent.TimeUnit;

@Slf4j
public class Ban extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("ban")){
            event.deferReply().queue();

            var target = event.getOption("username").getAsUser();
            var usrSnowflake = UserSnowflake.fromId(target.getId());

            try {
                var reason = event.getOption("reason").getAsString();
                ban(event, usrSnowflake, reason);
            } catch (NullPointerException e) {
                ban(event, usrSnowflake, "No reason provided.");
            }
        }
    }

    private void ban(SlashCommandInteractionEvent event, UserSnowflake usrSnowflake, String reason){
        //TODO for acexcy: add error handling tried it last night not working idk atm
        event.getGuild()
                .ban(usrSnowflake, 0, TimeUnit.MINUTES)
                .reason(reason)
                .queue(success -> {
                    //TODO add your frontend logic here
                    event.getGuild().getChannelById(TextChannel.class ,BotConfig.DARWIN_CHANNEL_ID)
                            .sendMessage("[DARWIN CHANNEL] Some dumbass got banned lulz heres his name: " + usrSnowflake.getAsMention() + " and the reason: " + reason)
                            .and(event.getHook().deleteOriginal())
                            .queue();
                });
    }
}
