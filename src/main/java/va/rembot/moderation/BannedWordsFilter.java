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
            // if so, exclude the whitelisted words from the original msg
            // and do another loop over this new list checking
            // if there are any banned words present
            if (hasWhitelistedCombinedWords(combinedWordsList)) {
                loopOverMsgExcludeWhitelist(combinedWordsList, substitutedMsg, event, msg);
                break;
            }

            // check message for a single whitelisted word afterward loop over list
            // excluding the whitelisted word and check again for banned words
            if (whitelistedWords.stream().anyMatch(s -> s.equals(word))) {
                loopOverMsgExcludeWhitelist(word, substitutedMsg, event, msg);
                break;
            }

            if (listBannedWords.stream().anyMatch(s -> s.equalsIgnoreCase(word))) {
                deleteMsg(event, msg, word);
                break;
            }
        }
    }

    private void deleteMsg(MessageReceivedEvent event, String msg, String bannedWord){
        log.info("A banned word was spotted in a message: {}", msg);
        log.info("The banned word was: {}", bannedWord);
        event.getMessage()
                .getChannel()
                .sendMessage("You said a banned word." + event.getAuthor().getAsMention())
                .and(event.getMessage().delete())
                .queue();
    }

    /// Loops over substituted message, for each word looks for any non-whitelisted combined word (2 words)
    /// AND if it is a banned word, if yes it calls the delete method
    private void loopOverMsgExcludeWhitelist(List<String> combinedWordsList, List<String> substituteMsg, MessageReceivedEvent event, String msg){

        var whitelistedWords = getWhitelistedWords(combinedWordsList);

        log.debug("INSIDE loopOverMsgExcludeWhitelist (combined whitelist word), substituteMsg: {}", substituteMsg);
        log.debug("INSIDE loopOverMsgExcludeWhitelist (combined whitelist word), whiteListedWords: {}", whitelistedWords);

        for (String word : substituteMsg){
            if (!whitelistedWords.contains(word) && listBannedWords.stream().anyMatch(s -> s.equalsIgnoreCase(word))){
                log.debug("NON-WHITELIST WORD: {}", word);

                deleteMsg(event, msg, word);
                break;
            }
        }
    }

    /// Loops over substituted message, for each word looks for any non-whitelisted word
    /// AND if it is a banned word, if yes it calls the delete method
    private void loopOverMsgExcludeWhitelist(String whitelistedWord, List<String> substituteMsg, MessageReceivedEvent event, String msg){

        log.debug("INSIDE loopOverMsgExcludeWhitelist (single whitelist word), substituteMsg: {}", substituteMsg);
        log.debug("INSIDE loopOverMsgExcludeWhitelist (single whitelist word), whitelistedWord: {}", whitelistedWord);

        for (String word : substituteMsg){
            if (!whitelistedWord.equals(word) && listBannedWords.stream().anyMatch(s -> s.equalsIgnoreCase(word))){
                log.debug("NON-WHITELIST WORD: {}", word);

                deleteMsg(event, msg, word);
                break;
            }
        }
    }

    /// returns true if input list has a whitelisted combined word
    private boolean hasWhitelistedCombinedWords(List<String> combinedWordsList){

        for (String word : combinedWordsList){
            if (whitelistedWords.stream().anyMatch(s -> s.equals(word))){
                return true;
            }
        }

        return false;
    }

    /// returns a list of strings of whitelisted combined words split up (e.g. ["Good Word"] becomes ["Good", "Word"]
    private List<String> getWhitelistedWords(List<String> combinedWordsList){

        List<String> whitelistedWordsList = new ArrayList<>();

        for (String combinedWord : combinedWordsList){
            if (whitelistedWords.contains(combinedWord)){
                log.debug("inside getWhiteListedWords method combined word: {}", Arrays.toString(combinedWord.split(" ")));

                for (String word : combinedWord.split(" ")){
                    log.debug("Whitelisted decombined word: {}", word);
                    whitelistedWordsList.add(word);
                }
            }
        }

        log.debug("returning whitelistedWordsList: {}", whitelistedWordsList);
        return whitelistedWordsList;
    }

    private List<String> getCombinedWords(List<String> substitutedMsg) {

        List<String> combinedWordsList = new ArrayList<>();
        String combinedWord;
        int comboWordIndexStart = 0;
        int comboWordIndexNext = 1;

        for (int i = 0; i< substitutedMsg.size(); i++){

            // combined word cant be one word
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

        //TODO should add support for when a substitute can be used for two or more chars (letters)
        // a scuffed solution could be to have a boolean that detects that and creates multiple words
        // puts them in the new substituted list and just hands them over to the delete msg bs above (draft idea)
        Map<Character, Character> subsForChars = new HashMap<>();
        subsForChars.put('@', 'a');
        subsForChars.put('4', 'a');
        subsForChars.put('3', 'e');
        subsForChars.put('9', 'g');
        subsForChars.put('1', 'i');
        subsForChars.put('|', 'i');
        subsForChars.put('!', 'i');
        subsForChars.put('¡', 'i');
        subsForChars.put('0', 'o');
        subsForChars.put('●', 'o');
        subsForChars.put('○', 'o');
        subsForChars.put('°', 'o');

        StringBuilder doubleAntiCensorChecker = new StringBuilder();
        boolean skipNext = false;

        for (String word : inputList) {

            char[] chars = word.toCharArray();
            newWord = "";

            //print out every word:
            log.debug(Arrays.toString(chars));

            for (int i = 0; i < word.length(); i++) {

                // need this for checking if double characters are a specific letter
                // such as () becomes the letter o
                if (skipNext){
                    skipNext = false;
                    continue;
                }

                doubleAntiCensorChecker.setLength(0);

                //print every character
                log.debug(String.valueOf(chars[i]));

                // if char is a suspected substitute:
                if (subsForChars.containsKey(chars[i])){
                    log.debug("Substitute char spotted: {}", subsForChars.get(chars[i]));
                    newWord += subsForChars.get(chars[i]);
                    continue;// skip this iteration
                }

                // this is for converting 2 chars into a letter
                try {
                    doubleAntiCensorChecker.append(chars[i]);
                    log.debug("first char: {}", doubleAntiCensorChecker);
                    doubleAntiCensorChecker.append(chars[i+1]);
                    log.debug("second char: {}", doubleAntiCensorChecker);

                    // add more if statements for 2 characters that can be converted to a letter
                    if (doubleAntiCensorChecker.toString().equals("()")){
                        newWord += "o";
                        skipNext = true; // skip another iteration because we merged 2 characters into 1
                        continue;
                    }
                    log.debug("is it in map?: {} another one: {}", subsForChars.get(chars[i]), subsForChars.get(chars[i]));
                } catch (ArrayIndexOutOfBoundsException e) {
                    log.debug(e.getMessage());
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
