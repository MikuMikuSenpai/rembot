package va.rembot.moderation.word_filter;

import lombok.extern.slf4j.Slf4j;
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
        if (LIST_BANNED_WORDS.isEmpty()) return;
        if (event.getAuthor().isBot()) return;
        if (ModerationLib.isMod(event.getMember())) return;

        String msg = event.getMessage().getContentRaw();
        msg = msg.replaceAll("https?://\\S+", "");
        String msgEmojisConvertedToChars = EmojiHelper.emojiToChar(msg);
        String msgEmojisConvertedToCharsTrimmed = msgEmojisConvertedToChars.replace(" ", "");

        String msgTotal = (msgEmojisConvertedToChars + " " + msgEmojisConvertedToCharsTrimmed);

        for (Character c : FILTERED_SPECIAL_CHARS)
            msgTotal = msgTotal.replace(c.toString(), "");

        String[] msgTotalArray = Arrays.stream(msgTotal.split(" "))
                .distinct()
                .filter(word -> !word.isEmpty())
                .toList().toArray(new String[0]);

        List<String> combinedWords = getCombinedWords(Arrays.stream(msgTotalArray).toList());

        log.debug("[onMessageReceived] msg: {}", msg);
        log.debug("[onMessageReceived] msgEmojisConvertedToChars: {}", msgEmojisConvertedToChars);
        log.debug("[onMessageReceived] msgEmojisConvertedToCharsTrimmed: {}", msgEmojisConvertedToCharsTrimmed);
        log.debug("[onMessageReceived] msgTotal: {}", msgTotal);
        log.debug("[onMessageReceived] msgTotalArray: {}", (Object) msgTotalArray);
        log.debug("[onMessageReceived] combinedWords: {}", combinedWords);
        log.debug("[onMessageReceived] LIST_BANNED_WORDS: {}", LIST_BANNED_WORDS);

        if (hasBannedWordExcludingWhitelistedWordsBeforeSubstituting(combinedWords, event, msgTotal)) return;

        boolean hasSubInMsg, hasDoubleAntiCensorChar, tooManySubCharsInMessage;
        hasSubInMsg = hasDoubleAntiCensorChar = tooManySubCharsInMessage = false;
        int countSubChars = 0;
        for (String word : msgTotalArray) {

            char[] wordAsCharArray = word.toCharArray();

            for (char character : wordAsCharArray) {

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

        log.debug("[onMessageReceived] tooManySubCharsInMessage: {}", tooManySubCharsInMessage);

        if (!hasSubInMsg && !hasDoubleAntiCensorChar || tooManySubCharsInMessage)
            return;

            //this is super spammy with a lot of false positives, keeping it for future but will likely never be used
//            if (tooManySubCharsInMessage) {
//
//                String userMention = event.getAuthor().getAsMention();
//                event.getJDA().getChannelById(TextChannel.class, BotConfig.LOG_CHANNEL_ID)
//                        .sendMessage("**[POTENTIAL BANNED WORD]** " + userMention + " <M: " + msg + ">").queue();
//                return;
//            }

        List<String> substitutedMsg = substitute(msgTotalArray);
        List<String> combinedWordsList = getCombinedWords(substitutedMsg);

        log.debug("[onMessageReceived] substitutedMsg: {}", substitutedMsg);
        log.debug("[onMessageReceived] combinedWordsList: {}", combinedWordsList);

        checkMessageForBannedWordExcludingWhitelisted(combinedWordsList, substitutedMsg, event, msg);
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

    private boolean hasBannedWordExcludingWhitelistedWordsBeforeSubstituting(List<String> combinedWordsList, MessageReceivedEvent event, String msg) {

        log.debug("[hasBannedWordExcludingWhitelistedWordsBeforeSubstituting] Checking for banned words before substituting combined words variant");

        String newMsg = msg;
        List<String> newMsgAsList;

        if (hasWhitelistedCombinedWords(combinedWordsList)) {
            for (String combinedWord : combinedWordsList) {
                if (WHITELISTED_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(combinedWord))) {
                    newMsg = newMsg.replaceAll(combinedWord, "");
                    log.debug("[hasBannedWordExcludingWhitelistedWordsBeforeSubstituting] whitelisted combined word found: {}", combinedWord);
                }
            }
        }

        newMsgAsList = Arrays.asList(newMsg.trim().split(" "));

        log.debug("[hasBannedWordExcludingWhitelistedWordsBeforeSubstituting] newMsg: {}", newMsg);
        log.debug("[hasBannedWordExcludingWhitelistedWordsBeforeSubstituting] newMsgAsList: {}", newMsgAsList);

        for (String word : newMsgAsList) {
            //should we check whitelist word case-sensitive? idk prob not... -_o_-
            if (WHITELISTED_WORDS.stream().noneMatch(s -> s.equalsIgnoreCase(word)) && LIST_BANNED_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(word))) {
                log.debug("[hasBannedWordExcludingWhitelistedWordsBeforeSubstituting] NON-WHITELIST BANNED WORD: {}", word);
                deleteMsg(event, msg, word);
                return true;
            }
        }

        return false;
    }

    private void checkMessageForBannedWordExcludingWhitelisted(List<String> combinedWordsList, List<String> substituteMsg, MessageReceivedEvent event, String msg) {

        StringBuilder sb = new StringBuilder();
        String newMessage;
        List<String> newMessageAsList;

        for (String word : substituteMsg)
            sb.append(word).append(" ");

        newMessage = sb.toString().trim();

        log.debug("[checkMessageForBannedWordExcludingWhitelisted] substituteMsg: {}", substituteMsg);
        log.debug("[checkMessageForBannedWordExcludingWhitelisted] newMessage: {}", newMessage);

        if (hasWhitelistedCombinedWords(combinedWordsList)) {
            for (String combinedWord : combinedWordsList) {
                if (WHITELISTED_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(combinedWord))) {
                    newMessage = newMessage.replaceAll(combinedWord, "");
                    log.debug("[checkMessageForBannedWordExcludingWhitelisted] whitelisted combined word found: {}", combinedWord);
                }
            }
        }

        newMessageAsList = Arrays.asList(newMessage.trim().split(" "));

        log.debug("[checkMessageForBannedWordExcludingWhitelisted] newMessageAsList: {}", newMessageAsList);

        for (String word : newMessageAsList) {
            if (WHITELISTED_WORDS.stream().noneMatch(s -> s.equalsIgnoreCase(word)) && LIST_BANNED_WORDS.stream().anyMatch(s -> s.equalsIgnoreCase(word))) {
                log.debug("[checkMessageForBannedWordExcludingWhitelisted] NON-WHITELIST BANNED WORD: {}", word);
                deleteMsg(event, msg, word);
                break;
            }
        }
    }

    private boolean hasWhitelistedCombinedWords(List<String> combinedWordsList) {

        for (String word : combinedWordsList)
            if (WHITELISTED_WORDS.stream().anyMatch(s -> s.equals(word)))
                return true;

        return false;
    }

    private List<String> getCombinedWords(List<String> substitutedMsg) {

        List<String> combinedWordsList = new ArrayList<>();
        String combinedWord;
        int comboWordIndexStart = 0;
        int comboWordIndexNext = 1;

        for (int i = 0; i < substitutedMsg.size(); i++) {

            // combined word cant be one word
            if (substitutedMsg.size() == 1) break;

            log.debug("[getCombinedWords] Combo word ONE: {}", substitutedMsg.get(comboWordIndexStart));
            log.debug("[getCombinedWords] Combo word TWO: {}", substitutedMsg.get(comboWordIndexNext));

            combinedWord = substitutedMsg.get(comboWordIndexStart) + " " + substitutedMsg.get(comboWordIndexNext);
            combinedWordsList.add(combinedWord);

            log.debug("[getCombinedWords] Combined word: {}", combinedWord);
            log.debug("[getCombinedWords] Combo word being build: {}", combinedWordsList);

            comboWordIndexStart += 1;
            comboWordIndexNext += 1;

            if (comboWordIndexStart >= substitutedMsg.size() || comboWordIndexNext + 1 >= substitutedMsg.size()) break;
        }

        log.debug("[getCombinedWords] The list being returned: {}", combinedWordsList);
        return combinedWordsList;
    }

    private List<String> substitute(String[] inputList) {

        List<String> newList = new ArrayList<>();
        Map<Character, List<Character>> subsForChars = SUBS_PER_CHAR;
        Map<Integer, Set<Character>> indexForSubs = new HashMap<>();
        Set<Character> potentialSubs = new HashSet<>();
        StringBuilder doubleAntiCensorChecker = new StringBuilder();
        StringBuilder newWord = new StringBuilder();
        int indexForSub = 0;
        boolean isSub = false;
        boolean isDoubleAntiCensorChar = false;

        for (String word : inputList) {

            char[] chars = word.toCharArray();
            newWord.setLength(0);
            indexForSubs.clear();
            potentialSubs.clear();

            log.debug("[substitute method] characters: {}", Arrays.toString(chars));
            log.debug("[substitute method] word: {}", word);

            for (int i = 0; i < word.length(); i++) {

                if (isDoubleAntiCensorChar) {
                    isDoubleAntiCensorChar = false;
                    continue;
                }

                doubleAntiCensorChecker.setLength(0);

                log.debug("[substitute] Every character {}", chars[i]);
                log.debug("[substitute] KEYS of subsForChars: {}", subsForChars.keySet());

                // example a, b, c, ... (letters)
                for (var key : subsForChars.keySet()) {
                    log.debug("[substitute] Each key (subsForChars): {}", key);

                    // example @, 4, ... (substitute chars/replacements)
                    for (var value : subsForChars.get(key)) {
                        log.debug("[substitute] Each value (subsForChars): {} for key: {}", value, key);

                        if (value.equals(chars[i])) {
                            log.debug("[substitute] Substitute char found: {} in word: {}", value, word);
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

                try {
                    doubleAntiCensorChecker.append(chars[i]);
                    log.debug("[substitute] First char (checking combined word): {}", doubleAntiCensorChecker);

                    if (i + 1 < chars.length)
                        doubleAntiCensorChecker.append(chars[i + 1]);
                    log.debug("[substitute] Second char (checking combined word): {}", doubleAntiCensorChecker);

                    if (doubleAntiCensorChecker.toString().equals("()")) {
                        newWord.append("o");
                        isDoubleAntiCensorChar = true;
                        continue;
                    }

                } catch (ArrayIndexOutOfBoundsException e) {
                    log.error("[substitute] ArrayIndexOutOfBoundsException {}", e.getMessage());
                }

                newWord.append(chars[i]);
                log.debug("[substitute] Current new word (building it): {}", newWord);
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

    public static void replaceSubWithChar(StringBuilder stringBuilder, int index, Map<Integer, Set<Character>> possibleSubs, List<String> newList) {

        if (index == stringBuilder.length()) {
            newList.add(String.valueOf(stringBuilder));
            log.debug("[replaceSubWithChar] newList replacing subs with normal chars: {}", newList);
            log.debug("[replaceSubWithChar] stringBuilder value at end: {}", stringBuilder);
            return;
        }

        if (possibleSubs.containsKey(index)) {

            char originalChar = stringBuilder.charAt(index);
            log.debug("[replaceSubWithChar] originalChar: {}", originalChar);

            for (Character sub : possibleSubs.get(index)) {
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
