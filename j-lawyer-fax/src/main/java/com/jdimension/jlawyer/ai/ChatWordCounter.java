package com.jdimension.jlawyer.ai;

import java.util.List;

/**
 * Counts the words of an AI chat.
 *
 * A word is a run of non-whitespace characters. All messages count, including tool calls and tool
 * results, because that is what is sent to the model again when a chat is continued. The number
 * indicates the size of a chat; it is not a token count.
 *
 * @author jens
 */
public class ChatWordCounter {

    private ChatWordCounter() {
    }

    /**
     * @param messages the messages of a chat, may be null
     * @return the number of words over the content of all messages
     */
    public static int countWords(List<Message> messages) {
        if (messages == null) {
            return 0;
        }
        int count = 0;
        for (Message m : messages) {
            if (m != null) {
                count += countWords(m.getContent());
            }
        }
        return count;
    }

    /**
     * @param text any text, may be null
     * @return the number of whitespace-separated words in it
     */
    public static int countWords(String text) {
        if (text == null) {
            return 0;
        }
        int count = 0;
        boolean inWord = false;
        for (int i = 0; i < text.length(); i++) {
            if (Character.isWhitespace(text.charAt(i))) {
                inWord = false;
            } else if (!inWord) {
                inWord = true;
                count++;
            }
        }
        return count;
    }
}
