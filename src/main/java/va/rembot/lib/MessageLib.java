package va.rembot.lib;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageLib {

    public static String extractMediaUrls(String message) {

        Pattern findMediaUrls = Pattern.compile(
                "(?:https?\\S+\\.avi\\S*|" +
                "https?\\S+\\.gif\\S*|" +
                "https?\\S+\\.heic\\S*|" +
                "https?\\S+\\.jpe?g\\S*|" +
                "https?\\S+\\.m4v\\S*|" +
                "https?\\S+\\.mkv\\S*|" +
                "https?\\S+\\.mov\\S*|" +
                "https?\\S+\\.mp4\\S*|" +
                "https?\\S+\\.png\\S*|" +
                "https?\\S+\\.svg\\S*|" +
                "https?\\S+\\.webm\\S*|" +
                "https?\\S+\\.webp\\S*|" +
                "https?://w{0,3}\\.?instagram\\.com/p/\\S+|" +
                "https?://w{0,3}\\.?instagram\\.com/reels/\\S+|" +
                "https?://w{0,3}\\.?kick\\.com/\\S+/clips/\\S+|" +
                "https?://w{0,3}\\.?klipy\\.com/gifs/\\S+|" +
                "https?://w{0,3}\\.?leetify\\.com/app/highlight/\\S+|" +
                "https?://w{0,3}\\.?medal\\.tv/games/\\S+|" +
                "https?://w{0,3}\\.?open\\.spotify\\.com/track/\\S+|" +
                "https?://w{0,3}\\.?reddit\\.com/r/\\S+/comments/\\S+|" +
                "https?://w{0,3}\\.?reddit\\.com/r/\\S+/s/\\S+|" +
                "https?://w{0,3}\\.?soundcloud\\.com/\\S+/\\S+|" +
                "https?://w{0,3}\\.?tiktok\\.com/\\S+/video/\\S+|" +
                "https?://w{0,3}\\.?twitch\\.tv/\\S+/clip/\\S+|" +
                "https?://w{0,3}\\.?twitter\\.com/\\S+/status/\\S+|" +
                "https?://w{0,3}\\.?vm\\.tiktok\\.com/\\S+|" +
                "https?://w{0,3}\\.?x\\.com/\\S+/status/\\S+|" +
                "https?://w{0,3}\\.?youtu\\.be/\\S+|" +
                "https?://w{0,3}\\.?youtube\\.com/watch\\?v=\\S+|" +
                ")", Pattern.CASE_INSENSITIVE);

        Matcher matcher = findMediaUrls.matcher(message);
        StringBuilder result = new StringBuilder();

        while (matcher.find())
            result.append(matcher.group()).append(" ");

        if (result.isEmpty())
            return "";
        else
            return result.toString().trim();
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
