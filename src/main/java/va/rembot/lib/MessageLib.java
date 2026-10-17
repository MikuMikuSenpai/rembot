package va.rembot.lib;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageLib {

    public static String extractMediaUrls(String message) {

        Pattern findMediaUrls = Pattern.compile("https?\\S+" +
                "(?:\\.avi|" +
                "\\.gif|" +
                "\\.heic|" +
                "\\.jpe?g|" +
                "\\.m4v|" +
                "\\.mkv|" +
                "\\.mov|" +
                "\\.mp4|" +
                "\\.png|" +
                "\\.svg|" +
                "\\.webm|" +
                "\\.webp|" +
                "klipy\\.com/gifs/\\S)\\S*", Pattern.CASE_INSENSITIVE);

        Matcher matcher = findMediaUrls.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find())
            result.append(matcher.group()).append(" ");

        if (result.isEmpty())
            return "";
        else
            return result.toString();
    }

    public static String removeUrls(String message) {
        return message.replaceAll("https?://\\S+", "");
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
