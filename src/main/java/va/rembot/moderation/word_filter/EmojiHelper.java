package va.rembot.moderation.word_filter;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class EmojiHelper {

    /// Converts all emojis (that are registered in emojiSubsMap()) to their corresponding character.
    public static String emojiToChar(String msg) {

        String normalMsg = msg;
        final Map<String, Character> emojiForChar = emojiSubsMap();

        for (Map.Entry<String, Character> entry : emojiForChar.entrySet()) {

            normalMsg = normalMsg.replace(entry.getKey(), String.valueOf(entry.getValue()));
        }

        log.debug("[emojiTurnedToChar] returning this new message: {}", normalMsg);

        return normalMsg;
    }

    private static Map<String, Character> emojiSubsMap() {

        // the keys are the UTF-16 Encodings (hexadecimal format) for each emoji
        // the values represent the normal char they replace
        // (just paste an emoji between "" and it should put the Unicode)
        HashMap<String, Character> emojiForChar = new HashMap<>();
        emojiForChar.put("\uD83C\uDDE6", 'a');
        emojiForChar.put("\uD83C\uDDE7", 'b');
        emojiForChar.put("\uD83C\uDDE8", 'c');
        emojiForChar.put("\uD83C\uDDE9", 'd');
        emojiForChar.put("\uD83C\uDDEA", 'e');
        emojiForChar.put("\uD83C\uDDEB", 'f');
        emojiForChar.put("\uD83C\uDDEC", 'g');
        emojiForChar.put("\uD83C\uDDED", 'h');
        emojiForChar.put("\uD83C\uDDEE", 'i');
        emojiForChar.put("\uD83C\uDDEF", 'j');
        emojiForChar.put("\uD83C\uDDF0", 'k');
        emojiForChar.put("\uD83C\uDDF1", 'l');
        emojiForChar.put("\uD83C\uDDF2", 'm');
        emojiForChar.put("\uD83C\uDDF3", 'n');
        emojiForChar.put("\uD83C\uDDF4", 'o');
        emojiForChar.put("\uD83C\uDDF5", 'p');
        emojiForChar.put("\uD83C\uDDF6", 'q');
        emojiForChar.put("\uD83C\uDDF7", 'r');
        emojiForChar.put("\uD83C\uDDF8", 's');
        emojiForChar.put("\uD83C\uDDF9", 't');
        emojiForChar.put("\uD83C\uDDFA", 'u');
        emojiForChar.put("\uD83C\uDDFB", 'v');
        emojiForChar.put("\uD83C\uDDFC", 'w');
        emojiForChar.put("\uD83C\uDDFD", 'x');
        emojiForChar.put("\uD83C\uDDFE", 'y');
        emojiForChar.put("\uD83C\uDDFF", 'z');
        emojiForChar.put("\uD83D\uDE2D", 'z');

        return emojiForChar;
    }
}
