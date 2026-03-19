package va.rembot.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.BotConfig;

import java.util.*;

@Slf4j
public class BannedWordsFilter extends ListenerAdapter {

    private static final List<String> listBannedWords = Arrays.stream(BotConfig.BANNED_WORDS_LIST).toList();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        //TODO add a exception list for example dont ban "Niger" = country

        var msg = event.getMessage().getContentRaw();
        var msgAsArray = msg.split(" ");

        var substitutedMsg = substitute(msgAsArray);

        for (String word : substitutedMsg){
            if (listBannedWords.stream().anyMatch(s -> s.equals(word))) {
                log.info("A banned word was spotted in a message: {}", msg);
                log.info("The banned word was: {}", word);
                event.getMessage().getChannel().sendMessage("You said a banned word." + event.getAuthor().getAsMention()).queue();
                event.getMessage().delete().queue();
                break;
            }
        }
    }

    /// 1. take an array of strings
    /// 2. loop over each word
    /// 3. loop over each char of the words and rebuild each word
    /// 4. if there is a potential substitute character replace it (e.g. @ becomes a)
    /// 5. put rebuild words in a new array and return it
    private List<String> substitute(String[] inputList){

        String newWord;
        List<String> newArray = new ArrayList<>();

        Map<Character, Character> subsForChars = new HashMap<>();
        subsForChars.put('@', 'a');
        subsForChars.put('4', 'a');
        subsForChars.put('3', 'e');
        subsForChars.put('1', 'i');
        subsForChars.put('|', 'i');

        for (String word : inputList) {

            char[] chars = word.toCharArray();
            newWord = "";

            //print out every word:
            log.debug(Arrays.toString(chars));

            for (int i = 0; i < word.length(); i++) {

                //print every character
                log.debug(String.valueOf(chars[i]));

                // if char is a suspected substitute:
                if (subsForChars.containsKey(chars[i])){
                    log.debug("Substitute char spotted: {}", subsForChars.get(chars[i]));
                    newWord += subsForChars.get(chars[i]);
                    continue;// skip this iteration
                }

                newWord += chars[i];
                log.debug("Current new word (building it): {}", newWord);
            }
            log.debug("New word: {}", newWord);
            newArray.add(newWord);
        }

        log.debug("New array: {}", newArray);
        return newArray;
    }
}
