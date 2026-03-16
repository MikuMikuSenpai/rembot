package va.rembot;

import net.dv8tion.jda.api.JDABuilder;

public class Bot {

    private static final String BOT_TOKEN = System.getenv("BOT_TOKEN");

    public static void main(String[] args) {
        JDABuilder
                .createDefault(BOT_TOKEN)
                .build();
    }
}
