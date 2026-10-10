package com.niuqu.chatbubble.chat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WhisperFormatsTest {
    @Test void keywordBeforeColonIsWhisper() {
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("Steve 悄悄对你说: hi"));
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("Steve whispers to you: hi"));
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("[私聊] Steve -> 你: hi"));
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("密语 Steve: hi"));
    }

    @Test void keywordAfterColonIsPublicChat() {
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("Steve: 为什么不能用私聊"));
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("Steve: I used /whisper"));
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("Steve：说说悄悄话"));
    }

    @Test void noColonWholeTextScanned() {
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("Steve whispers to you"));
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("Steve says hello"));
    }

    @Test void tpaRequestIsNotWhisper() {
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon(
            "[Essentials] melankol427 wants to teleport to you.  [Yes]  [No]"));
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon(
            "[Essentials] Steve is trying to teleport to you"));
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon(
            "[Essentials] Bob has requested to teleport to you"));

        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("Steve whispers to you: hi"));
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("Steve PM you: hi"));
    }

    @Test void pluginKeywordBeforeColonIsWhisper() {
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("私信 Steve: hi"));
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("[密谈] Steve: hi"));
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("Steve 密谈: hi"));
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("Steve PM you: hi"));
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("Steve message to you: hi"));
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("Steve msg you: hi"));
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("Steve tell you: hi"));
    }

    @Test void pluginKeywordCaseInsensitive() {
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("Steve pm you: hi"));
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("Steve Pm: hi"));
    }

    @Test void shortEnglishWordsNeedWordBoundary() {
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("Steve hepm: hi"));
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("Steve msgbox: hi"));
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("Steve teller: hi"));
    }

    @Test void pluginKeywordAfterColonStillPublicChat() {
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("Steve: 加我私信"));
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("Steve: send me a PM"));
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("Steve: I used /msg"));
    }

    @Test void playerNamedAfterKeywordIsPublicChat() {
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("Msg: 大家好"));
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("Tell: hi everyone"));
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("pm: hello"));
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("Message: hi"));
    }

    @Test void bracketPrefixKeywordIsPublicChat() {
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("[PM]Steve: 大家好"));
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("[TELL] Alex: hi"));
        assertFalse(MessagePresentation.hasWhisperKeywordBeforeColon("[MSG]Bob: hi"));
    }

    @Test void keywordWithRealStructureStillWhisper() {
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("Steve PM you: hi"));
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("PM Steve: hi"));
        assertTrue(MessagePresentation.hasWhisperKeywordBeforeColon("Steve msg you: hi"));
    }

    @Test void extractsAfterColon() {
        assertEquals("hi", MessagePresentation.extractWhisperContent("Steve 悄悄对你说: hi", "Steve"));
    }

    @Test void multiColonContentKeptWhole() {
        assertEquals("看这句: 引用", MessagePresentation.extractWhisperContent(
            "Steve 悄悄对你说: 看这句: 引用", "Steve"));
    }

    @Test void arrowFormatExtractsAtColon() {
        assertEquals("hi", MessagePresentation.extractWhisperContent("Steve -> you: hi", "Steve"));
    }

    @Test void chevronFormatWithoutColon() {
        assertEquals("hi", MessagePresentation.extractWhisperContent("Steve >> hi", "Steve"));
    }

    @Test void noSeparatorTrims() {
        assertEquals("hi there", MessagePresentation.extractWhisperContent("Steve hi there", "Steve"));
    }
}
