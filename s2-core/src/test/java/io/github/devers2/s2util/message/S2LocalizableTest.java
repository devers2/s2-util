package io.github.devers2.s2util.message;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Locale;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import io.github.devers2.s2util.exception.S2Exception;
import io.github.devers2.s2util.exception.S2RuntimeException;

class S2LocalizableTest {

    private static final String BASENAME = "s2test.errors";

    @AfterEach
    void reset() {
        S2ResourceBundle.resetDefaultBasename();
    }

    private static S2RuntimeException exists(String name) {
        return new S2RuntimeException("File already exists: /srv/upload/" + name,
                S2MessageKey.of(BASENAME, "file.exists", name));
    }

    @Test
    void theMessageFollowsTheUserLanguageWhileGetMessageStaysEnglish() {
        var e = exists("보고서.pdf");
        assertEquals("File already exists: /srv/upload/보고서.pdf", e.getMessage());
        assertEquals("보고서.pdf은(는) 이미 있는 파일입니다.", e.getLocalizedMessage(Locale.KOREA),
                "a Latin last letter gives both forms");
        assertEquals("사진은 이미 있는 파일입니다.", exists("사진").getLocalizedMessage(Locale.KOREAN), "josa follows the word");
        assertEquals("보고서.pdf already exists.", e.getLocalizedMessage(Locale.ENGLISH));
        assertEquals("보고서.pdf already exists.", e.getLocalizedMessage(Locale.JAPANESE), "no translation: the base file");
    }

    @Test
    void anEnglishRequestOnAKoreanServerGetsEnglish() {
        var saved = Locale.getDefault();
        Locale.setDefault(Locale.KOREA);
        try {
            // ResourceBundle.getBundle would fall back to the server's ko bundle here | getBundle 은 여기서 서버의 ko 로 대체함
            assertEquals("a.txt already exists.", exists("a.txt").getLocalizedMessage(Locale.US));
        } finally {
            Locale.setDefault(saved);
        }
    }

    @Test
    void theApplicationBundleOverridesTheLibraryText() {
        S2ResourceBundle.setDefaultBasename("s2test/app");
        var e = new S2RuntimeException("Queue full", S2MessageKey.of(BASENAME, "queue.full"));
        assertEquals("앱이 덮어쓴 문구", e.getLocalizedMessage(Locale.KOREAN));
        assertEquals("The server is busy. Please try again later.", e.getLocalizedMessage(Locale.ENGLISH),
                "the app has no English override, so the library text is used");
    }

    @Test
    void withoutAKeyOrATranslationTheEnglishMessageIsUsed() {
        assertEquals("plain", new S2RuntimeException("plain").getLocalizedMessage(Locale.KOREAN));
        assertNull(new S2RuntimeException("plain").getMessageKey());
        var missing = new S2Exception("English", S2MessageKey.of(BASENAME, "no.such.key"));
        assertEquals("English", missing.getLocalizedMessage(Locale.KOREAN));
        assertTrue(S2MessageKey.of("no.such.bundle", "x").resolve(Locale.KOREAN).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> S2MessageKey.of(BASENAME, " "));
    }

    @Test
    void findLocalizedMessageLooksThroughWrappingExceptions() {
        // The wrapper has its own key, but the cause is more specific | 감싼 예외도 키가 있지만 원인이 더 구체적
        var wrapped = new S2RuntimeException("Cannot upload: a.txt", exists("a.txt"),
                S2MessageKey.of(BASENAME, "queue.full"));
        assertEquals("a.txt은(는) 이미 있는 파일입니다.", S2Localizable.findLocalizedMessage(wrapped, Locale.KOREAN).orElseThrow());
        assertTrue(S2Localizable.findLocalizedMessage(new IllegalStateException("internal"), Locale.KOREAN).isEmpty(),
                "no user-facing message: the caller shows a generic one");

        // Any exception type can take part, for example a RejectedExecutionException | 어떤 예외 종류든 참여 가능
        class Rejected extends RejectedExecutionException implements S2Localizable {
            private static final long serialVersionUID = 1L;

            Rejected() {
                super("Queue is full");
            }

            @Override
            public S2MessageKey getMessageKey() {
                return S2MessageKey.of(BASENAME, "queue.full");
            }
        }
        assertEquals("서버가 바쁩니다. 잠시 후 다시 시도하세요.",
                S2Localizable.findLocalizedMessage(new RuntimeException(new Rejected()), Locale.KOREAN).orElseThrow());
    }

    @Test
    void anExceptionWithAKeyIsSerializable() throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var out = new ObjectOutputStream(bytes)) {
            out.writeObject(exists("b.txt"));
        }
        try (var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            var copy = (S2RuntimeException) in.readObject();
            assertEquals("b.txt already exists.", copy.getLocalizedMessage(Locale.ENGLISH));
        }
    }

    @Test
    void failuresWithoutAKeyGetAGenericMessage() {
        var internal = new IllegalStateException("opacity must be between 0 and 1");
        assertEquals("처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.", S2Localizable.localizedMessageOrDefault(internal, Locale.KOREAN));
        assertEquals("Something went wrong. Please try again later.", S2Localizable.localizedMessageOrDefault(internal, Locale.ENGLISH));
        assertEquals("a.txt already exists.", S2Localizable.localizedMessageOrDefault(exists("a.txt"), Locale.ENGLISH));
    }
}
