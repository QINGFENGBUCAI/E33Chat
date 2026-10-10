package com.niuqu.chatbubble.chat;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class MentionDetectorTest {

    @Test
    public void messageStartingWithPlayerName_requireAtOff_doesNotCrash() {

        assertTrue(MentionDetector.isMentioned("Steve hello", "steve", false, null));
    }

    @Test
    public void atMention_requireAtOff_detected() {
        assertTrue(MentionDetector.isMentioned("hey @steve", "steve", false, null));
    }

    @Test
    public void nameInsideLongerWord_notMentioned() {

        assertFalse(MentionDetector.isMentioned("xsteve hello", "steve", false, null));
    }

    @Test
    public void nameWithSuffixLetter_notMentioned() {

        assertFalse(MentionDetector.isMentioned("stevex hello", "steve", false, null));
    }

    @Test
    public void noAt_requireAtOn_notMentioned() {
        assertFalse(MentionDetector.isMentioned("steve hello", "steve", true, null));
    }

    @Test
    public void atMention_requireAtOn_detected() {
        assertTrue(MentionDetector.isMentioned("@steve hello", "steve", true, null));
    }

    @Test
    public void emptyOrNullText_notMentioned() {
        assertFalse(MentionDetector.isMentioned("", "steve", false, null));
        assertFalse(MentionDetector.isMentioned(null, "steve", false, null));
    }

    @Test
    public void replyFromSelf_detected() {
        assertTrue(MentionDetector.isMentioned("anything", "steve", true, "steve"));
    }
}
