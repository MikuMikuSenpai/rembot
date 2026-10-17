package va.rembot.commands.non_slash;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.*;

public class Nuke extends ListenerAdapter {

    static Map<String, String> nukableCountries = new LinkedHashMap<>();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().equals("&nuke")) {
            if (event.getAuthor().isBot()) return;

            HashMap<String, String> nukedCountry = getANukableCountry();
            String country = "";
            String link = "";

            for (String key : nukedCountry.keySet()) {
                country = key;
                link = nukedCountry.get(key);
            }

            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle("u NUKED " + country + " gg");
            eb.setImage(link);
            eb.setColor(0xbb0a1e);

            event.getMessage().getChannel().sendMessageEmbeds(eb.build()).queue();
        }
    }

    private static HashMap<String, String> getANukableCountry() {
        nukableCountries.put("France", "https://i.ibb.co/dwQQb4qK/nukeparis.jpg");
        nukableCountries.put("India", "https://i.ibb.co/7J0wQXCm/india-nuked.jpg");
        nukableCountries.put("Russia", "https://i.ibb.co/jk5M2PVq/russia-nuked.png");
        nukableCountries.put("Saudi Arabia", "https://i.ibb.co/qYJsxLNj/saudi-arabia-nuked.jpg");

        List<String> countries = List.of("France", "India", "Russia", "Saudi Arabia");

        int size = countries.size();
        Random random = new Random();
        int randomInt = random.nextInt(size);

        String randomCountry = countries.get(randomInt);
        String randomCountryImage = nukableCountries.get(randomCountry);

        return new HashMap<>(Map.of(randomCountry, randomCountryImage));
    }
}
