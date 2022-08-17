package io.quarkiverse.logging.cloudwatch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.LogRecord;

import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;

class LoggingCloudWatchHandlerTest {

    private final LoggingCloudWatchHandler testee = new LoggingCloudWatchHandler();

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
