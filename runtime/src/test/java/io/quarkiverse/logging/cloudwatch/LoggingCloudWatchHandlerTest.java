package io.quarkiverse.logging.cloudwatch;

import static org.junit.jupiter.api.Assertions.*;

import java.util.logging.LogRecord;

import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;

class LoggingCloudWatchHandlerTest {

    private final LoggingCloudWatchHandler testee = new LoggingCloudWatchHandler();

    @Test
    void shouldExtractNextSequenceToken() {
        Exception e = new RuntimeException(
                "The given sequenceToken is invalid. The next expected sequenceToken is: 49611889230645343415657219696041834137426496333583221330 (Service: AWSLogs; Status Code: 400; Error Code: InvalidSequenceTokenException; Request ID: 02120a3d-bc00-45df-96fd-b3491ba01924; Proxy: null)");
        String exceptionMessage = e.getMessage();
        final String actual = testee.extractValidSequenceToken(exceptionMessage);

        assertEquals("49611889230645343415657219696041834137426496333583221330", actual);
    }

    @Test
    void shouldExtractNextSequenceTokenWhenThereAreBlanks() {
        Exception e = new RuntimeException(
                "The given sequenceToken is invalid. The next expected sequenceToken is:  49611889230645343415657219696041834137426496333583221330  (Service: AWSLogs; Status Code: 400; Error Code: InvalidSequenceTokenException; Request ID: 02120a3d-bc00-45df-96fd-b3491ba01924; Proxy: null)");
        String exceptionMessage = e.getMessage();
        final String actual = testee.extractValidSequenceToken(exceptionMessage);

        assertEquals("49611889230645343415657219696041834137426496333583221330", actual);
    }

    @Test
    void shouldBeBelowThresholdWhenBothAreInfo() {
        LogRecord record = new LogRecord(Level.INFO, "someMessage");
        testee.setLevel(Level.INFO);
        assertFalse(testee.isBelowThreshold(record));
    }

    @Test
    void shouldBeBelowThresholdWhenLogRecordIsWarnAndHandlerLevelIsInfo() {
        LogRecord record = new LogRecord(Level.WARN, "someMessage");
        testee.setLevel(Level.INFO);
        assertFalse(testee.isBelowThreshold(record));
    }

    @Test
    void shouldBeBelowThresholdWhenLogRecordIsInfoAndHandlerLevelIsWarn() {
        LogRecord record = new LogRecord(Level.INFO, "someMessage");
        testee.setLevel(Level.WARN);
        assertTrue(testee.isBelowThreshold(record));
    }

    @Test
    void shouldBeAboveThresholdWhenLogRecordIsWarnAndHandlerLevelIsSevere() {
        LogRecord record = new LogRecord(Level.WARN, "someMessage");
        testee.setLevel(Level.SEVERE);
        assertTrue(testee.isBelowThreshold(record));
    }

    @Test
    void shouldBeAboveThresholdWhenLogRecordIsInfoAndHandlerLevelIsSevere() {
        LogRecord record = new LogRecord(Level.INFO, "someMessage");
        testee.setLevel(Level.SEVERE);
        assertTrue(testee.isBelowThreshold(record));
    }
}
