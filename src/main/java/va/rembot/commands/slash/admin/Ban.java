package va.rembot.commands.slash.admin;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.exceptions.ErrorHandler;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import va.rembot.BotConfig;

import java.awt.*;
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
                var embedMsg = buildEmbed(target, reason);
                ban(event, usrSnowflake, reason, slashCommandUser, target, embedMsg);
            } catch (NullPointerException e) {
                var reason = "No reason provided.";
                var embedMsg = buildEmbed(target, reason);
                ban(event, usrSnowflake, reason, slashCommandUser, target, embedMsg);
            }
        }
    }

    private MessageEmbed buildEmbed(User targetUser, String reason){
        EmbedBuilder embed = new EmbedBuilder();
        //TODO if you want embed message make it here do same for other classes if we repeat a lot in diff classes
        // we can make one generic method for this in BotConfig
        embed.addField("TestField", "TestValue", true); // cant create empty embed so this some filler (delete when u start)
        /*
        heres a guide on what u can change: https://raw.githubusercontent.com/discord-jda/JDA/assets/assets/docs/embeds/01-Overview.png
        use below as example:
        embed.setColor(0xbb0a1e); //hexadecimal color needs to start w "0x"
        embed.setTitle("Someone got banned");
        embed.addField("User", targetUser.getAsMention(), true);
        embed.addField("Reason", reason, false);
        embed.setFooter("rekt xd");
        embed.setTimestamp(Instant.now());
         */
        return embed.build();
    }

    private void ban(SlashCommandInteractionEvent event, UserSnowflake usrSnowflake, String reason, User slashCommandUser, User targetUser, MessageEmbed embed){
        event.getGuild()
                .ban(usrSnowflake, 0, TimeUnit.MINUTES)
                .reason(reason)
                .queue(success -> {
                    //TODO add your frontend logic here
                    event.getGuild().getChannelById(TextChannel.class ,BotConfig.DARWIN_CHANNEL_ID)
                            .sendMessage("[DARWIN CHANNEL] Some dumbass got banned lulz heres his name: " + usrSnowflake.getAsMention() + " and the reason: " + reason)
                            .and(event.getGuildChannel().sendMessageEmbeds(embed))
                            .and(event.getHook().deleteOriginal())
                            .queue();
                }, new ErrorHandler()
                        .handle(ErrorResponse.MISSING_PERMISSIONS, e -> {
                            log.error("Bot doesn't have enough permissions to ban the target user. (Bot probably has a lower or same discord role hierarchy as the target).");
                            log.error("{} tried to ban {}", slashCommandUser, targetUser);
                            event.getHook()
                                    .editOriginal("Failed to ban that user because I don't have sufficient perms (most likely need a role with higher permissions than the target)." + slashCommandUser.getAsMention())
                                    .queue();
                                })
                        .handle(ErrorResponse.UNKNOWN_USER, e -> {
                            log.error("Dont know who the target user is (Unknown user).");
                            log.error("{} tried to ban {}", slashCommandUser, targetUser);
                            event.getHook()
                                    .editOriginal("I can't find the person you are trying to ban (unknown user)." + slashCommandUser.getAsMention())
                                    .queue();
                        }));
    }
}
