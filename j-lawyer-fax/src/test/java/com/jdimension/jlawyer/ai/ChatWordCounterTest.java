package com.jdimension.jlawyer.ai;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

/**
 * How the word count of an AI chat is determined.
 *
 * @author jens
 */
public class ChatWordCounterTest {

    private static Message message(String role, String content) {
        Message m = new Message();
        m.setRole(role);
        m.setContent(content);
        return m;
    }

    @Test
    public void emptyAndNullInputsCountZero() {
        assertEquals(0, ChatWordCounter.countWords((List<Message>) null));
        assertEquals(0, ChatWordCounter.countWords(new ArrayList<>()));
        assertEquals(0, ChatWordCounter.countWords((String) null));
        assertEquals(0, ChatWordCounter.countWords(""));
        assertEquals(0, ChatWordCounter.countWords("  \n\t "));
    }

    @Test
    public void whitespaceRunsSeparateWords() {
        assertEquals(1, ChatWordCounter.countWords("Hallo"));
        assertEquals(3, ChatWordCounter.countWords("  Frist  berechnen\n\tbitte  "));
        assertEquals(2, ChatWordCounter.countWords("§ 517"));
    }

    @Test
    public void allMessagesCountIncludingToolMessages() {
        Message nullContent = message(Message.ROLE_ASSISTANT, null);
        List<Message> chat = Arrays.asList(
                message(Message.ROLE_USER, "Wann endet die Frist?"),
                message(Message.ROLE_ASSISTANT, "Ich sehe nach."),
                message(Message.ROLE_TOOL, "{\"deadline\": \"2026-10-20\"}"),
                nullContent,
                null,
                message(Message.ROLE_ASSISTANT, "Am 20.10.2026."));
        assertEquals(4 + 3 + 2 + 0 + 0 + 2, ChatWordCounter.countWords(chat));
    }
}
