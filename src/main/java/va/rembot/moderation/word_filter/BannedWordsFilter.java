package va.rembot.moderation.word_filter;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import va.rembot.BotConfig;

import java.util.*;

@Slf4j
public class BannedWordsFilter extends ListenerAdapter {

    private static final List<String> listBannedWords = Arrays.stream(BotConfig.BANNED_WORDS_LIST).toList();
    private static final List<String> whitelistedWords = Arrays.stream(BotConfig.WHITELISTED_WORDS_LIST).toList();

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        var msg = event.getMessage().getContentRaw();
        var msgEmojisConvertedToChars = EmojiHelper.emojiToChar(msg);
        var msgEmojisConvertedToCharsTrimmed = msgEmojisConvertedToChars.replaceAll(" ", ""); // its not possible to handle all cases as the input will be highly variable (e.g. a edge case now would be that "w ord w ord" is turned into "wordword") i will implement a method in botconfig that multiplies words for an x amount (x=not yet decided)
        var msgAsArray = msg.split(" ");
        var msgAsArrayTrimmed = Arrays.stream(msgAsArray).filter(word -> !word.isEmpty()).toArray(String[]::new);
        var msgAsArrayAsList = Arrays.stream(msgAsArrayTrimmed).toList();

        StringBuilder msgAsArrayAsListString = new StringBuilder();
        msgAsArrayAsList.forEach(msgAsArrayAsListString::append);

        var msgTotal = msgAsArrayAsListString + " " + msgEmojisConvertedToChars + " " + msgEmojisConvertedToCharsTrimmed;
        var msgTotalArray = msgTotal.split(" ");
        var msgTotalArrayTrimmed = Arrays.stream(msgTotalArray).filter(word -> !word.isEmpty()).toArray(String[]::new);
        var substitutedMsg = substitute(msgTotalArrayTrimmed);
        var combinedWordsList = getCombinedWords(substitutedMsg);

        log.debug("[onMessageReceived] msg: {}", msg);
        log.debug("[onMessageReceived] msgEmojisConvertedToChars: {}", msgEmojisConvertedToChars);
        log.debug("[onMessageReceived] msgEmojisConvertedToCharsTrimmed: {}", msgEmojisConvertedToCharsTrimmed);
        log.debug("[onMessageReceived] msgAsArray: {}", (Object) msgAsArray);
        log.debug("[onMessageReceived] msgAsArrayTrimmed: {}", (Object) msgAsArrayTrimmed);
        log.debug("[onMessageReceived] msgAsArrayAsList: {}", msgAsArrayAsList);
        log.debug("[onMessageReceived] msgTotal: {}", msgTotal);
        log.debug("[onMessageReceived] msgTotalArray: {}", (Object) msgTotalArray);
        log.debug("[onMessageReceived] msgTotalArrayTrimmed: {}", (Object) msgTotalArrayTrimmed);
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
        List<String> newList = new ArrayList<>();

        // keep these alphabetically sorted (on keys [letters]) for ease
        Map<Character, List<Character>> subsForChars = new HashMap<>();
        subsForChars.put('a', List.of('@', '4', '^'));
        subsForChars.put('o', List.of('0', '●', '○', '°', '@'));

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

                //print every character
                log.debug(String.valueOf(chars[i]));

                // if char is a suspected substitute:
                log.debug("KEYS of subsForChars: {}", subsForChars.keySet());

                // example a, b, c, ... (letters)
                for (var key : subsForChars.keySet()){
                    log.debug("Each key (subsForChars): {}", key);

                    // example @, 4, ... (substitute chars/replacements)
                    for (var value : subsForChars.get(key)){
                        log.debug("Each value (subsForChars): {} for key: {}", value, key);

                        if (value.equals(chars[i])){
                            log.debug("Substitute char found: {} in word: {}", value, word);
                            potentialSubs.add(key);
                            indexForSub = i;
                            isSub = true;
                        }
                    }
                }

                if (isSub)
                    indexForSubs.put(indexForSub, potentialSubs);
                log.debug("indexForSubs BUILDING IT: {}", indexForSubs);
                log.debug("potentialSubs: {}", potentialSubs);
                log.debug("Current index: {}", i);


                // this is for converting 2 chars into a letter
                try {

                    doubleAntiCensorChecker.append(chars[i]);
                    doubleAntiCensorChecker.append(chars[i + 1]);

                    log.debug("First char (checking combined word): {}", doubleAntiCensorChecker);
                    log.debug("Second char (checking combined word): {}", doubleAntiCensorChecker);

                    // add more if statements for 2 characters that can be converted to a letter
                    if (doubleAntiCensorChecker.toString().equals("()")){
                        newWord += "o";
                        skip = true; // skip another iteration because we merged 2 characters into 1
                        continue;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    log.debug(e.getMessage());
                }

                newWord += chars[i];
                log.debug("Current new word (building it): {}", newWord);
            }

            isSub = false;

            StringBuilder stringBuilder = new StringBuilder(newWord);
            replaceSubWithChar(stringBuilder, 0, indexForSubs, newList);

            log.debug("newList: {}", newList);
            log.debug("FINAL newWord: {}", newWord);

            newList.add(newWord);
        }

        log.debug("Returning this newList: {}", newList);
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
            log.debug("New array replacing subs with normal chars: {}", newList);
            log.debug("stringBuilder value at end: {}", stringBuilder);
            return;
        }

        if (possibleSubs.containsKey(index)){

            char originalChar = stringBuilder.charAt(index);
            log.debug("originalChar: {}", originalChar);

            for (Character sub : possibleSubs.get(index)){
                stringBuilder.setCharAt(index, sub);
                log.debug("sub: {}", sub);

                replaceSubWithChar(stringBuilder, index + 1, possibleSubs, newList);
            }

            stringBuilder.setCharAt(index, originalChar);
        } else {
            replaceSubWithChar(stringBuilder, index + 1, possibleSubs, newList);
        }
    }
}
