package va.rembot.lib;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ExtractFromMessage {

    public static String extractMediaUrls(String message) {

        Pattern findMediaUrls = Pattern.compile("https?\\S+" +
                "(?:\\.avi|" +
                "\\.gif|" +
                "\\.heic|" +
                "\\.jpe?g|" +
                "\\.mkv|" +
                "\\.mov|" +
                "\\.mp4|" +
                "\\.png|" +
                "\\.webm|" +
                "\\.webp)\\S*", Pattern.CASE_INSENSITIVE);

        Matcher matcher = findMediaUrls.matcher(message);

        if (matcher.find())
            return matcher.group();
        else
            return "";
    }

    public static boolean endsWithUrl(String message) {
        Pattern pattern = Pattern.compile("https?://\\S+$");
        Matcher matcher = pattern.matcher(message);

        return matcher.find();
    }

    public static boolean hasDiscordInviteLink(String message) {

        Pattern pattern = Pattern.compile("\\S*discord(?:\\.gg|\\.com\\/invite)\\S+");
        Matcher matcher = pattern.matcher(message);

        return matcher.find();
    }
}
