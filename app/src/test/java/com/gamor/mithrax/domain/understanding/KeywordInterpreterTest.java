package com.gamor.mithrax.domain.understanding;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class KeywordInterpreterTest {

    private final KeywordInterpreter interpreter = new KeywordInterpreter();

    @Test
    public void greetsHello() {
        assertEquals("Hi there! How can I help you today?", interpreter.interpret("hello"));
    }

    @Test
    public void handlesEmptyTranscript() {
        assertEquals("I didn't catch that.", interpreter.interpret(""));
    }

    @Test
    public void answersTime() {
        assertTrue(interpreter.interpret("what time is it").startsWith("The current time is "));
    }
}
