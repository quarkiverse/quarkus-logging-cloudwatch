package io.quarkiverse.logging.cloudwatch;

import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;

import java.util.logging.LogRecord;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingCloudWatchHandlerTest {

    private final LoggingCloudWatchHandler testee = new LoggingCloudWatchHandler();

    @Test
    void shouldFormatNormalLogMessage() {
        LogRecord record = new LogRecord(Level.WARN, "Uh oh! This error should not occur in production! :(");
        testee.setLevel(Level.WARN);

        String formattedMessage = testee.formatMessage(record);

        assertTrue(formattedMessage.contains("message\":\"Uh oh! This error should not occur in production! :("));
    }

    @Test
    void shouldFormatPercentageAsWell() {
        LogRecord record = new LogRecord(Level.INFO, "Progress: 10%");
        testee.setLevel(Level.INFO);

        String formattedMessage = testee.formatMessage(record);

        assertTrue(formattedMessage.contains("Progress: 10%"));
    }

    @Test
    void shouldFormatPercentageAndReplacePlaceholder() {
        // e.g. log.info("info logging: %", info)
        LogRecord record = new LogRecord(Level.INFO, "Progress: %s%%");
        record.setParameters(new Object[]{"1337"});
        testee.setLevel(Level.INFO);

        String formattedMessage = testee.formatMessage(record);

        assertTrue(formattedMessage.contains("Progress: 1337%"));
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
