package va.rembot.moderation.word_filter;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.BotConfig;

import java.util.*;

@Slf4j
public class BannedWordsFilter extends ListenerAdapter {

    private static final List<String> LIST_BANNED_WORDS = Arrays.stream(BotConfig.BANNED_WORDS_LIST).toList();
    private static final List<String> WHITELISTED_WORDS = Arrays.stream(BotConfig.WHITELISTED_WORDS_LIST).toList();
    private static final Map<Character, List<Character>> SUBS_PER_CHAR = getSubsForChars();
    private static final Set<Character> SUBSTITUTE_CHARS = new HashSet<>();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        var member = event.getMember();
        var modRole = event.getJDA().getRoleById(BotConfig.getMOD_ROLE_ID_LONG());

        if (member.getUnsortedRoles().contains(modRole)) {
            log.debug("[onMessageReceived] This user is mod, banned words filter not applied.");
            return;
        }

        var msg = event.getMessage().getContentRaw();
        var msgEmojisConvertedToChars = EmojiHelper.emojiToChar(msg);
        var msgEmojisConvertedToCharsTrimmed = msgEmojisConvertedToChars.replaceAll(" ", "");
        var msgAsArray = msg.split(" ");
        var msgAsArrayTrimmed = Arrays.stream(msgAsArray).filter(word -> !word.isEmpty()).toArray(String[]::new);
        var msgAsArrayAsList = Arrays.stream(msgAsArrayTrimmed).toList();

        StringBuilder msgAsArrayAsListString = new StringBuilder();
        msgAsArrayAsList.forEach(msgAsArrayAsListString::append);

        var msgTotal = msgAsArrayAsListString + " " + msgEmojisConvertedToChars + " " + msgEmojisConvertedToCharsTrimmed;
        var msgTotalArray = msgTotal.split(" ");
        var msgTotalArrayTrimmed = Arrays.stream(msgTotalArray).filter(word -> !word.isEmpty()).toArray(String[]::new);

        // skip the heavy (slow) substitute method if possible
        if (checkMsgBeforeSubstituting(msgTotalArray, event, msg))
            return;

        //the below two for loops are used to check if there are any substitute chars in current msg
        // if NOT skip checking for banned words since we did that above
        // letters: a, b, c,...
        for (var key : SUBS_PER_CHAR.keySet()) {

            log.debug("[onMessageReceived] letter: {}", key.toString());

            // substitute chars: @, 4, !, ...
            for (var value : SUBS_PER_CHAR.get(key)) {

                log.debug("[onMessageReceived] substitute char: {}", value.toString());
                SUBSTITUTE_CHARS.add(value);

            }
        }

        var hasSubInMsg = false;
        for (var word : msgTotalArray) {

            var charArray = word.toCharArray();

            //same as foreach char of word
            for (var character : charArray){

                log.debug("[onMessageReceived] character {}", character);

                if (SUBSTITUTE_CHARS.contains(character)) {
                    log.debug("[onMessageReceived] Substitute char detected");
                    hasSubInMsg = true;
                }
            }
        }

        log.debug("[onMessageReceived] msg: {}", msg);
        log.debug("[onMessageReceived] msgEmojisConvertedToChars: {}", msgEmojisConvertedToChars);
        log.debug("[onMessageReceived] msgEmojisConvertedToCharsTrimmed: {}", msgEmojisConvertedToCharsTrimmed);
        log.debug("[onMessageReceived] msgAsArray: {}", (Object) msgAsArray);
        log.debug("[onMessageReceived] msgAsArrayTrimmed: {}", (Object) msgAsArrayTrimmed);
        log.debug("[onMessageReceived] msgAsArrayAsList: {}", msgAsArrayAsList);
        log.debug("[onMessageReceived] msgTotal: {}", msgTotal);
        log.debug("[onMessageReceived] msgTotalArray: {}", (Object) msgTotalArray);
        log.debug("[onMessageReceived] msgTotalArrayTrimmed: {}", (Object) msgTotalArrayTrimmed);
        log.debug("[onMessageReceived] LIST_BANNED_WORDS: {}", LIST_BANNED_WORDS);

        //check if there are banned words obfuscated by whitespaces
        for (var word : msgTotalArrayTrimmed) {

            log.debug("[onMessageReceived] word: {}", word);

            if (LIST_BANNED_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(word))) {
                deleteMsg(event, msg, word);
                log.debug("[onMessageReceived] Word is banned! {}", word);
                break;
            }
        }

        if (!hasSubInMsg)
            return;

        var substitutedMsg = substitute(msgTotalArrayTrimmed);
        var combinedWordsList = getCombinedWords(substitutedMsg);

        log.debug("[onMessageReceived] substitutedMsg: {}", substitutedMsg);
        log.debug("[onMessageReceived] combinedWordsList: {}", combinedWordsList);

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
            if (WHITELISTED_WORDS.stream().anyMatch(s -> s.equals(word))) {
                loopOverMsgExcludeWhitelist(word, substitutedMsg, event, msg);
                break;
            }

            if (LIST_BANNED_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(word))) {
                deleteMsg(event, msg, word);
                break;
            }
        }
    }

    /// returns true if there is a banned word spotted in msg without substituting
    /// this results in significantly faster deleting of the message
    private boolean checkMsgBeforeSubstituting(String[] msgTotal, MessageReceivedEvent event, String originalMsgRaw) {

        var combinedWords = getCombinedWords(Arrays.stream(msgTotal).toList());
        var msgTotalList = Arrays.stream(msgTotal).toList();

        log.debug("[checkMsgBeforeSubstituting] msgTotal {}", (Object) msgTotal);
        log.debug("[checkMsgBeforeSubstituting] combinedWords {}", combinedWords);
        log.debug("[checkMsgBeforeSubstituting] msgTotalList {}", msgTotalList);

        // this is copied from above (inside onMessageReceived method), for more info read above
        for (var word : msgTotalList){

            if (hasWhitelistedCombinedWords(combinedWords)) {
                return loopOverMsgExcludeWhitelistBoolean(combinedWords, msgTotalList, event, originalMsgRaw);
            }

            if (WHITELISTED_WORDS.stream().anyMatch(s -> s.equals(word))) {
                return loopOverMsgExcludeWhitelistBoolean(word, msgTotalList, event, originalMsgRaw);
            }

            if (LIST_BANNED_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(word))) {
                deleteMsg(event, originalMsgRaw, word);
                return true;
            }

        }

        return false;
    }

    private void deleteMsg(MessageReceivedEvent event, String msg, String bannedWord){
        log.info("[deleteMsg] A banned word was spotted in a message: {}", msg);
        log.info("[deleteMsg] The banned word was: {}", bannedWord);
        event.getMessage()
                .getChannel()
                .sendMessage("You said a banned word." + event.getAuthor().getAsMention())
                .and(event.getMessage().delete())
                .queue();
    }

    /// This variant is made for checkMsgBeforeSubstituting method
    /// Loops over msgTotal, for each word looks for any non-whitelisted combined word (2 words)
    /// AND if it is a banned word, if yes it calls the delete method AND returns true (so that substitute method is skipped)
    private boolean loopOverMsgExcludeWhitelistBoolean(List<String> combinedWordsList, List<String> msgTotalList, MessageReceivedEvent event, String msg){

        var whitelistedWords = getWhitelistedWords(combinedWordsList);

        log.debug("[loopOverMsgExcludeWhitelistBoolean] (combined whitelist word) msgTotalList: {}", msgTotalList);
        log.debug("[loopOverMsgExcludeWhitelistBoolean] (combined whitelist word) whiteListedWords: {}", whitelistedWords);

        for (String word : msgTotalList){
            if (!whitelistedWords.contains(word) && LIST_BANNED_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(word))){
                log.debug("[loopOverMsgExcludeWhitelistBoolean] NON-WHITELIST WORD: {}", word);

                deleteMsg(event, msg, word);
                return true;
            }
        }

        return false;
    }

    /// This variant is made for checkMsgBeforeSubstituting method
    /// Loops over msgTotal, for each word looks for any non-whitelisted word
    /// AND if it is a banned word, if yes it calls the delete method AND returns true (so that substitute method is skipped)
    private boolean loopOverMsgExcludeWhitelistBoolean(String whitelistedWord, List<String> msgTotalList, MessageReceivedEvent event, String msg){

        log.debug("[loopOverMsgExcludeWhitelistBoolean] (single whitelist word) msgTotalList: {}", msgTotalList);
        log.debug("[loopOverMsgExcludeWhitelistBoolean] (single whitelist word) whitelistedWord: {}", whitelistedWord);

        for (String word : msgTotalList){
            if (!whitelistedWord.equals(word) && LIST_BANNED_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(word))){
                log.debug("[loopOverMsgExcludeWhitelistBoolean] NON-WHITELIST WORD: {}", word);

                deleteMsg(event, msg, word);
                return true;
            }
        }

        return false;
    }

    /// Loops over substituted message, for each word looks for any non-whitelisted combined word (2 words)
    /// AND if it is a banned word, if yes it calls the delete method
    private void loopOverMsgExcludeWhitelist(List<String> combinedWordsList, List<String> substituteMsg, MessageReceivedEvent event, String msg){

        var whitelistedWords = getWhitelistedWords(combinedWordsList);

        log.debug("[loopOverMsgExcludeWhitelist] (combined whitelist word) substituteMsg: {}", substituteMsg);
        log.debug("[loopOverMsgExcludeWhitelist] (combined whitelist word) whiteListedWords: {}", whitelistedWords);

        for (String word : substituteMsg){
            if (!whitelistedWords.contains(word) && LIST_BANNED_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(word))){
                log.debug("[loopOverMsgExcludeWhitelist] NON-WHITELIST WORD: {}", word);

                deleteMsg(event, msg, word);
                break;
            }
        }
    }

    /// Loops over substituted message, for each word looks for any non-whitelisted word
    /// AND if it is a banned word, if yes it calls the delete method
    private void loopOverMsgExcludeWhitelist(String whitelistedWord, List<String> substituteMsg, MessageReceivedEvent event, String msg){

        log.debug("[loopOverMsgExcludeWhitelist] (single whitelist word) substituteMsg: {}", substituteMsg);
        log.debug("[loopOverMsgExcludeWhitelist] (single whitelist word) whitelistedWord: {}", whitelistedWord);

        for (String word : substituteMsg){
            if (!whitelistedWord.equals(word) && LIST_BANNED_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(word))){
                log.debug("[loopOverMsgExcludeWhitelist] NON-WHITELIST WORD: {}", word);

                deleteMsg(event, msg, word);
                break;
            }
        }
    }

    /// returns true if input list has a whitelisted combined word
    private boolean hasWhitelistedCombinedWords(List<String> combinedWordsList){

        for (String word : combinedWordsList){
            if (WHITELISTED_WORDS.stream().anyMatch(s -> s.equals(word))){
                return true;
            }
        }

        return false;
    }

    /// returns a list of strings of whitelisted combined words split up (e.g. ["Good Word"] becomes ["Good", "Word"]
    private List<String> getWhitelistedWords(List<String> combinedWordsList){

        List<String> whitelistedWordsList = new ArrayList<>();

        for (String combinedWord : combinedWordsList){
            if (WHITELISTED_WORDS.contains(combinedWord)){
                log.debug("[getWhitelistedWords] combined word: {}", Arrays.toString(combinedWord.split(" ")));

                for (String word : combinedWord.split(" ")){
                    log.debug("[getWhitelistedWords] Whitelisted decombined word: {}", word);
                    whitelistedWordsList.add(word);
                }
            }
        }

        log.debug("[getWhitelistedWords] returning whitelistedWordsList: {}", whitelistedWordsList);
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

            log.debug("[getCombinedWords] Combo word ONE: {}", substitutedMsg.get(comboWordIndexStart));
            log.debug("[getCombinedWords] Combo word TWO: {}", substitutedMsg.get(comboWordIndexNext));

            combinedWord = substitutedMsg.get(comboWordIndexStart) + " " + substitutedMsg.get(comboWordIndexNext);
            combinedWordsList.add(combinedWord);

            log.debug("[getCombinedWords] Combined word: {}", combinedWord);
            log.debug("[getCombinedWords] Combo word being build: {}", combinedWordsList);

            comboWordIndexStart += 1;
            comboWordIndexNext += 1;

            // prevent out of bounds error when going through list:
            if (comboWordIndexStart >= substitutedMsg.size() || comboWordIndexNext + 1 >= substitutedMsg.size())
                break;
        }

        log.debug("[getCombinedWords] The list being returned: {}", combinedWordsList);
        return combinedWordsList;
    }

    /// 1. take an array of strings
    /// 2. loop over each word
    /// 3. loop over each char of the words and rebuild each word
    /// 4. if there is a potential substitute character replace it (e.g. @ becomes a)
    /// 5. put rebuild words in a new array and return it
    private List<String> substitute(String[] inputList){

        String newWord;
        List<String> newList = new ArrayList<>();

        // keep these alphabetically sorted (on keys [letters]) for ease
        Map<Character, List<Character>> subsForChars = SUBS_PER_CHAR;

        // per index can be multiple subs
        Map<Integer, Set<Character>> indexForSubs = new HashMap<>();
        // prevent duplicates
        Set<Character> potentialSubs = new HashSet<>();
        int indexForSub = 0;
        boolean isSub = false;
        StringBuilder doubleAntiCensorChecker = new StringBuilder();
        boolean skip = false;

        for (String word : inputList) {

            char[] chars = word.toCharArray();
            newWord = "";
            indexForSubs.clear();
            potentialSubs.clear();

            log.debug("[substitute method] characters: {}", Arrays.toString(chars));
            log.debug("[substitute method] word: {}", word);

            for (int i = 0; i < word.length(); i++) {

                // need this for checking if double characters are a specific letter
                // such as () becomes the letter o
                if (skip){
                    skip = false;
                    continue;
                }

                doubleAntiCensorChecker.setLength(0);

                log.debug("[substitute] Every character {}", chars[i]);

                // if char is a suspected substitute:
                log.debug("[substitute] KEYS of subsForChars: {}", subsForChars.keySet());

                // example a, b, c, ... (letters)
                for (var key : subsForChars.keySet()){
                    log.debug("[substitute] Each key (subsForChars): {}", key);

                    // example @, 4, ... (substitute chars/replacements)
                    for (var value : subsForChars.get(key)){
                        log.debug("[substitute] Each value (subsForChars): {} for key: {}", value, key);

                        if (value.equals(chars[i])){
                            log.debug("[substitute] char found: {} in word: {}", value, word);
                            potentialSubs.add(key);
                            indexForSub = i;
                            isSub = true;
                        }
                    }
                }

                if (isSub)
                    indexForSubs.put(indexForSub, potentialSubs);
                log.debug("[substitute] indexForSubs BUILDING IT: {}", indexForSubs);
                log.debug("[substitute] potentialSubs: {}", potentialSubs);
                log.debug("[substitute] Current index: {}", i);


                // this is for converting 2 chars into a letter
                try {

                    doubleAntiCensorChecker.append(chars[i]);
                    doubleAntiCensorChecker.append(chars[i + 1]);

                    log.debug("[substitute] First char (checking combined word): {}", doubleAntiCensorChecker);
                    log.debug("[substitute] Second char (checking combined word): {}", doubleAntiCensorChecker);

                    // add more if statements for 2 characters that can be converted to a letter
                    if (doubleAntiCensorChecker.toString().equals("()")){
                        newWord += "o";
                        skip = true; // skip another iteration because we merged 2 characters into 1
                        continue;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    log.debug("[substitute] ArrayIndexOutOfBoundsException {}", e.getMessage());
                }

                newWord += chars[i];
                log.debug("[substitute] Current new word (building it): {}", newWord);
            }

            isSub = false;

            StringBuilder stringBuilder = new StringBuilder(newWord);
            replaceSubWithChar(stringBuilder, 0, indexForSubs, newList);

            log.debug("[substitute] newList: {}", newList);
            log.debug("[substitute] FINAL newWord: {}", newWord);

            newList.add(newWord);
        }

        log.debug("[substitute] Returning this newList: {}", newList);
        return newList;
    }

    /// This method is essential part of substitute and does the main work it uses backtracking to make each variant
    /// 1. check if index is string's length = we checked every character
    /// 2. Check if current index (starts with 0) has any substitute characters by using the possibleSubs map
    /// 2.1. If there is a substitute character use the stored set and loop over each one and replace them
    ///   (this is done recursively by calling the same method again added a higher index of 1)
    /// 2.2. If there is no substitute char at this index just go to next index
    /// 3. When done back at the first if put in newList
    /// /!\ if a message is huge AND it contains potential substitute chars this will throw a java.lang.OutOfMemoryError
    /// I m not sure how to handle this atm, basically the message will just be sent to disc and not handled properly
    /// this is also pretty slow for big messages but it works so I'll keep it,
    /// need to find some way to make this more performant in the future
    public static void replaceSubWithChar(StringBuilder stringBuilder, int index, Map<Integer, Set<Character>> possibleSubs, List<String> newList){

        if (index == stringBuilder.length()){
            newList.add(String.valueOf(stringBuilder));
            log.debug("[replaceSubWithChar] newList replacing subs with normal chars: {}", newList);
            log.debug("[replaceSubWithChar] stringBuilder value at end: {}", stringBuilder);
            return;
        }

        if (possibleSubs.containsKey(index)){

            char originalChar = stringBuilder.charAt(index);
            log.debug("[replaceSubWithChar] originalChar: {}", originalChar);

            for (Character sub : possibleSubs.get(index)){
                stringBuilder.setCharAt(index, sub);
                log.debug("[replaceSubWithChar] sub: {}", sub);

                replaceSubWithChar(stringBuilder, index + 1, possibleSubs, newList);
            }

            stringBuilder.setCharAt(index, originalChar);
        } else {
            replaceSubWithChar(stringBuilder, index + 1, possibleSubs, newList);
        }
    }

    /// returns current subs supported per character
    private static Map<Character, List<Character>> getSubsForChars() {
        Map<Character, List<Character>> newMap = new HashMap<>();
        newMap.put('a', List.of('@', '4', '^'));
        newMap.put('o', List.of('0', '●', '○', '°', '@'));

        return newMap;
    }
}
