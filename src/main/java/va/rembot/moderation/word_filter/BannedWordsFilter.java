package va.rembot.moderation.word_filter;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.BotConfig;
import va.rembot.lib.ModerationLib;

import java.util.*;

@Slf4j
public class BannedWordsFilter extends ListenerAdapter {

    private static final List<String> LIST_BANNED_WORDS = Arrays.stream(BotConfig.BANNED_WORDS_ARRAY).toList();
    private static final List<String> WHITELISTED_WORDS = Arrays.stream(BotConfig.WHITELISTED_WORDS_ARRAY).toList();
    private static final Map<Character, List<Character>> SUBS_PER_CHAR = getSubsForChars();
    private static final Set<Character> DOUBLE_ANTI_CENSOR_CHARS = getPotentialDoubleAntiCensor();
    private static final Set<Character> FILTERED_SPECIAL_CHARS = getFilteredSpecialChars();
    private static final Set<Character> SUBSTITUTE_CHARS = getAllSubstituteChars();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;
        if (LIST_BANNED_WORDS.isEmpty()) return;
        if (ModerationLib.isMod(event.getMember())) return;

        String msg = event.getMessage().getContentRaw();
        //exclude links for potential false positive
        msg = msg.replaceAll("https?:\\/\\/\\S+", "");
        String msgEmojisConvertedToChars = EmojiHelper.emojiToChar(msg);
        String msgEmojisConvertedToCharsTrimmed = msgEmojisConvertedToChars.replaceAll(" ", "");

        String msgTotal = (msgEmojisConvertedToChars + " " + msgEmojisConvertedToCharsTrimmed);

        for (Character c : FILTERED_SPECIAL_CHARS)
            msgTotal = msgTotal.replace(c.toString(), "");

        String[] msgTotalArray = Arrays.stream(msgTotal.split(" ")).distinct().toList().toArray(new String[0]);
        String[] msgTotalArrayTrimmed = Arrays.stream(msgTotalArray).filter(word -> !word.isEmpty()).toArray(String[]::new);

        if (checkMsgBeforeSubstituting(msgTotalArray, event, msg))
            return;

        boolean hasSubInMsg = false;
        boolean hasDoubleAntiCensorChar = false;
        int countSubChars = 0;
        boolean tooManySubCharsInMessage = false;
        for (String word : msgTotalArray) {

            char[] wordAsCharArray = word.toCharArray();

            for (char character : wordAsCharArray){

                if (SUBSTITUTE_CHARS.contains(character)) {
                    hasSubInMsg = true;
                    countSubChars++;
                }

                if (DOUBLE_ANTI_CENSOR_CHARS.contains(character))
                    hasDoubleAntiCensorChar = true;

            }
        }

        if (countSubChars > BotConfig.getAllowedAmountSubstituteCharactersPerMessage())
            tooManySubCharsInMessage = true;

        log.debug("[onMessageReceived] msg: {}", msg);
        log.debug("[onMessageReceived] msgEmojisConvertedToChars: {}", msgEmojisConvertedToChars);
        log.debug("[onMessageReceived] msgEmojisConvertedToCharsTrimmed: {}", msgEmojisConvertedToCharsTrimmed);
        log.debug("[onMessageReceived] msgTotal: {}", msgTotal);
        log.debug("[onMessageReceived] msgTotalArray: {}", (Object) msgTotalArray);
        log.debug("[onMessageReceived] msgTotalArrayTrimmed: {}", (Object) msgTotalArrayTrimmed);
        log.debug("[onMessageReceived] LIST_BANNED_WORDS: {}", LIST_BANNED_WORDS);
        log.debug("[onMessageReceived] tooManySubCharsInMessage: {}", tooManySubCharsInMessage);

        if (!hasSubInMsg && !hasDoubleAntiCensorChar || tooManySubCharsInMessage) {

            if (tooManySubCharsInMessage) {

                String userMention = event.getAuthor().getAsMention();
                event.getJDA().getChannelById(TextChannel.class, BotConfig.LOG_CHANNEL_ID)
                        .sendMessage("**[POTENTIAL BANNED WORD]** " + userMention + " <M: " + msg + ">").queue();
                return;
            }

            return;
        }

        List<String> substitutedMsg = substitute(msgTotalArrayTrimmed, event);
        List<String> combinedWordsList = getCombinedWords(substitutedMsg);

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

    /// check b4 substituting = fast!
    private boolean checkMsgBeforeSubstituting(String[] msgTotal, MessageReceivedEvent event, String originalMsgRaw) {

        List<String> msgTotalList = Arrays.stream(msgTotal).toList();
        List<String> combinedWords = getCombinedWords(msgTotalList);

        log.debug("[checkMsgBeforeSubstituting] msgTotal {}", (Object) msgTotal);
        log.debug("[checkMsgBeforeSubstituting] combinedWords {}", combinedWords);
        log.debug("[checkMsgBeforeSubstituting] msgTotalList {}", msgTotalList);

        if (hasWhitelistedCombinedWords(combinedWords))
            return hasBannedWordExcludingWhitelistedWordsBeforeSubstituting(combinedWords, event, originalMsgRaw);

        // this is copied from above (inside onMessageReceived method), for more info read above
        for (String word : msgTotalList) {

            if (WHITELISTED_WORDS.stream().anyMatch(s -> s.equals(word)))
                return hasBannedWordExcludingWhitelistedWordsBeforeSubstituting(word, msgTotalList, event, originalMsgRaw);

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

    /// combined word variant
    private boolean hasBannedWordExcludingWhitelistedWordsBeforeSubstituting(List<String> combinedWordsList, MessageReceivedEvent event, String msg) {

        log.debug("[hasBannedWordExcludingWhitelistedWordsBeforeSubstituting] Checking for banned words before substituting combined words variant");

        String newMsg = "";
        List<String> newMsgAsList;

        for (String combinedWord : combinedWordsList) {
            if (WHITELISTED_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(combinedWord))) {
                newMsg = msg.replaceAll(combinedWord, "");
                log.debug("[hasBannedWordExcludingWhitelistedWordsBeforeSubstituting] whitelisted combined word found: {}", combinedWord);
            }
        }

        newMsgAsList = Arrays.asList(newMsg.trim().split(" "));

        log.debug("[hasBannedWordExcludingWhitelistedWordsBeforeSubstituting] newMsg: {}", newMsg);
        log.debug("[hasBannedWordExcludingWhitelistedWordsBeforeSubstituting] newMsgAsList: {}", newMsgAsList);

        for (String word : newMsgAsList){
            //should we check whitelist word case-sensitive? idk prob not... -_o_-
            if (!WHITELISTED_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(word)) && LIST_BANNED_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(word))){
                log.debug("[hasBannedWordExcludingWhitelistedWordsBeforeSubstituting] NON-WHITELIST BANNED WORD: {}", word);
                deleteMsg(event, msg, word);
                return true;
            }
        }

        return false;
    }

    /// singular word variant
    private boolean hasBannedWordExcludingWhitelistedWordsBeforeSubstituting(String whitelistedWord, List<String> msgTotalList, MessageReceivedEvent event, String msg){

        log.debug("[hasBannedWordExcludingWhitelistedWordsBeforeSubstituting] (single whitelist word) msgTotalList: {}", msgTotalList);
        log.debug("[hasBannedWordExcludingWhitelistedWordsBeforeSubstituting] (single whitelist word) whitelistedWord: {}", whitelistedWord);

        for (String word : msgTotalList){
            if (!whitelistedWord.equals(word) && LIST_BANNED_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(word))){
                log.debug("[hasBannedWordExcludingWhitelistedWordsBeforeSubstituting] NON-WHITELIST BANNED WORD: {}", word);

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
                log.debug("[loopOverMsgExcludeWhitelist] NON-WHITELIST BANNED WORD: {}", word);

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
                log.debug("[loopOverMsgExcludeWhitelist] NON-WHITELIST BANNED WORD: {}", word);

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

        for (int i = 0; i < substitutedMsg.size(); i++){

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
    /// 4. if there is a potential substitute character put it in a Map
    /// with Integer and List Character (substitutes that are possible per index of word)
    /// 5. Send each word to replaceSubWithChar() with the Map and make all variants
    /// of the word with their normal characters
    private List<String> substitute(String[] inputList, MessageReceivedEvent event){

        int amountOfSubWords = 0;
        String newWord;
        List<String> newList = new ArrayList<>();

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
            newWord = ""; //not really new word unless double anti censor keep var name unless better found
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
                            log.debug("[substitute] Substitute char found: {} in word: {}", value, word);
                            potentialSubs.add(key);
                            indexForSub = i;
                            isSub = true;
                        }
                    }
                }

                if (isSub) {
                    indexForSubs.put(indexForSub, potentialSubs);
                }
                log.debug("[substitute] indexForSubs BUILDING IT: {}", indexForSubs);
                log.debug("[substitute] potentialSubs: {}", potentialSubs);
                log.debug("[substitute] Current index: {}", i);


                // this is for converting 2 chars into a letter
                try {

                    doubleAntiCensorChecker.append(chars[i]);
                    log.debug("[substitute] First char (checking combined word): {}", doubleAntiCensorChecker);

                    if (i + 1 < chars.length)
                        doubleAntiCensorChecker.append(chars[i + 1]);
                    log.debug("[substitute] Second char (checking combined word): {}", doubleAntiCensorChecker);

                    // add more if statements for 2 characters that can be converted to a letter
                    if (doubleAntiCensorChecker.toString().equals("()")){
                        newWord += "o";
                        skip = true; // skip another iteration because we merged 2 characters into 1
                        continue;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    log.error("[substitute] ArrayIndexOutOfBoundsException {}", e.getMessage());
                }

                newWord += chars[i];
                log.debug("[substitute] Current new word (building it): {}", newWord);
            }

            if (isSub)
                amountOfSubWords++;

            // we can change amountOfSubWords to any number but higher = less performant
            // we do + 1 because we also include a concatenated message of the original
            // for example original message "badword badword2" would become "badword" "badword2" "badwordbadword2"
            if (isSub && amountOfSubWords > BotConfig.getSubstituteBannedWordCheckAmountInt() + 1) {
                log.debug("[substitute] At least one word found with substitute char, only checking this word.");

                //we should put this in lib/extracted method see issue #35 and #40
                var user = event.getAuthor().getAsMention();
                var msgRaw = event.getMessage().getContentRaw();

                event.getJDA().getChannelById(TextChannel.class, BotConfig.LOG_CHANNEL_ID)
                        .sendMessage("**[POTENTIAL BANNED WORD]** " + user + " <M: " + msgRaw + ">").queue();

                break;
            }

            StringBuilder stringBuilder = new StringBuilder(newWord);
            replaceSubWithChar(stringBuilder, 0, indexForSubs, newList);

            log.debug("[substitute] newList: {}", newList);
            log.debug("[substitute] FINAL newWord: {}", newWord);

            isSub = false;
        }

        log.debug("[substitute] Returning this newList: {}", newList);
        return newList;
    }

    /// THIS HAPPENS WORD PER WORD FROM SUBSTITUTE()
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

    private static Map<Character, List<Character>> getSubsForChars() {
        Map<Character, List<Character>> subsForChars = new HashMap<>();
        subsForChars.put('a', List.of('@', '4', '^'));
        subsForChars.put('e', List.of('3', '€'));
        subsForChars.put('i', List.of('!', '¡', '|', '1'));
        subsForChars.put('o', List.of('0', '●', '○', '°', '@'));

        return subsForChars;
    }

    private static HashSet<Character> getAllSubstituteChars() {
        return new HashSet<>(Set.of('@', '4', '^', '3', '€', '!', '¡', '|', '1', '0', '●', '○', '°'));
    }

    private static Set<Character> getPotentialDoubleAntiCensor() {
        Set<Character> doubleAntiCensorChars = new HashSet<>();
        doubleAntiCensorChars.add('(');
        doubleAntiCensorChars.add(')');

        return doubleAntiCensorChars;
    }

    private static Set<Character> getFilteredSpecialChars() {
        Set<Character> filteredSpecialChars = new HashSet<>();
        filteredSpecialChars.add('\"');
        filteredSpecialChars.add('-');
        filteredSpecialChars.add('\'');
        filteredSpecialChars.add('~');

        return filteredSpecialChars;
    }
}
