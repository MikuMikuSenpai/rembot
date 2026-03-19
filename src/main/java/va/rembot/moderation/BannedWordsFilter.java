package va.rembot.moderation;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.BotConfig;

import java.util.*;

@Slf4j
public class BannedWordsFilter extends ListenerAdapter {

    private static final List<String> listBannedWords = Arrays.stream(BotConfig.BANNED_WORDS_LIST).toList();
    private static final List<String> whitelistedWords = Arrays.stream(BotConfig.WHITELISTED_WORDS_LIST).toList();

    /// TODO ADD AUTO BAN (DO THIS AT THE LAST STAGE OF BANNNED WORDS FILTER RECHECK W MIKU)
    ///
    /// TODO ADD DOCUMENTATION FOR EACH METHOD (MANY MOVING PARTS WHERE THINGS CAN BREAK)
    ///
    /// TODO CHANGE LOGGERS TO DEBUG WHERE NEEDED OR ADD MORE
    ///
    /// TODO EXTRACT THE DELETE MESSAGE EVENT CUS ITS REPEATED TWICE
    ///
    /// TODO REVIEW DOCUMENT CHANGE SILLY VAR NAMES OR METHOD NAMES WHERE NEEDED BUT WILL BE RECHECKED IN PR SO NOT BIGGEST DEAL ATM
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        var msg = event.getMessage().getContentRaw();
        var msgAsArray = msg.split(" ");
        var substitutedMsg = substitute(msgAsArray);
        var combinedWordsList = getCombinedWords(substitutedMsg);

        for (String word : substitutedMsg) {

            // check if msg has any combined words that are whitelisted
            if (hasWhitelistedCombinedWords(combinedWordsList)){
                loopOverMsgExcludeWhitelist(combinedWordsList, substitutedMsg, event, msg);
            break;
            }

            // check message for a single whitelisted word
            if (whitelistedWords.stream().anyMatch(s -> s.equals(word)))
                break;

            if (listBannedWords.stream().anyMatch(s -> s.equalsIgnoreCase(word))) {
                log.info("A banned word was spotted in a message: {}", msg);
                log.info("The banned word was: {}", word);
                event.getMessage()
                        .getChannel()
                        .sendMessage("You said a banned word." + event.getAuthor().getAsMention())
                        .and(event.getMessage().delete())
                        .queue();
                break;
            }
        }
    }

    /// this one was added for combined words support (reminder to ace for later)
    private void loopOverMsgExcludeWhitelist(List<String> combinedWordsList, List<String> substituteMsg, MessageReceivedEvent event, String msg){

        var whiteListedWords = getWhiteListedWords(combinedWordsList);

        log.info("INSIDE loopOverMsgExcludeWhitelist, substituteMsg: {}", substituteMsg);
        log.info("INSIDE loopOverMsgExcludeWhitelist, whiteListedWords: {}", whiteListedWords);

        for (String word : substituteMsg){
            if (!whiteListedWords.contains(word) && listBannedWords.stream().anyMatch(s -> s.equalsIgnoreCase(word))){
                log.info("NONWHITELIST WORD: {}", word);

                log.info("A banned word was spotted in a message: {}", msg);
                log.info("The banned word was: {}", word);
                event.getMessage()
                        .getChannel()
                        .sendMessage("You said a banned word." + event.getAuthor().getAsMention())
                        .and(event.getMessage().delete())
                        .queue();
                break;
            }
        }
    }

    private boolean hasWhitelistedCombinedWords(List<String> combinedWordsList){

        for (String word : combinedWordsList){
            if (whitelistedWords.stream().anyMatch(s -> s.equals(word))){
                return true;
            }
        }

        return false;
    }

    /// this one was added for combined words support (reminder to ace for later)
    private List<String> getWhiteListedWords(List<String> combinedWordsList){

        List<String> whitelistedWordsList = new ArrayList<>();

        for (String combinedWord : combinedWordsList){
            if (whitelistedWords.contains(combinedWord)){
                log.info("getWhiteListedWords: {}", Arrays.toString(combinedWord.split(" ")));

                for (String word : combinedWord.split(" ")){
                    log.info("Whitelisted decombined word: {}", word);
                    whitelistedWordsList.add(word);
                }
            }
        }

        log.info("whitelistedWordsList: {}", whitelistedWordsList);
        return whitelistedWordsList;
    }

    private List<String> getCombinedWords(List<String> substitutedMsg) {

        List<String> combinedWordsList = new ArrayList<>();
        String combinedWord;
        int comboWordIndexStart = 0;
        int comboWordIndexNext = 1;

        for (int i = 0; i< substitutedMsg.size(); i++){

            if (substitutedMsg.size() == 1)
                break;

            log.debug("Combo word ONE: {}", substitutedMsg.get(comboWordIndexStart));
            log.debug("Combo word TWO: {}", substitutedMsg.get(comboWordIndexNext));

            combinedWord = substitutedMsg.get(comboWordIndexStart) + " " + substitutedMsg.get(comboWordIndexNext);
            combinedWordsList.add(combinedWord);

            log.debug("Combined word: {}", combinedWord);
            log.debug("Combo word being build: {}", combinedWordsList);

            comboWordIndexStart += 1;
            comboWordIndexNext += 1;

            // prevent out of bounds error when going through list:
            if (comboWordIndexStart >= substitutedMsg.size() || comboWordIndexNext + 1 >= substitutedMsg.size())
                break;
        }

        log.debug("The list being returned: {}", combinedWordsList);
        return combinedWordsList;
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
