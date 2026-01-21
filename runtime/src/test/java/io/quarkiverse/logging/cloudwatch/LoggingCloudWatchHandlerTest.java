package io.quarkiverse.logging.cloudwatch;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.logging.LogRecord;

import org.apache.commons.lang3.RandomStringUtils;
import org.jboss.logmanager.Level;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsResponse;

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
        record.setParameters(new Object[] { "1337" });
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

    @Test
    void shouldTruncateTooLongMessages() {
        LogRecord record = new LogRecord(Level.INFO, RandomStringUtils.secure().next(1000));
        testee.setLevel(Level.INFO);

        String formattedMessage = testee.formatMessage(record);

        assertTrue(formattedMessage.length() > 1000);

        LoggingCloudWatchHandler testeeWithMessageLimit = new LoggingCloudWatchHandler(null, null, null, null, Optional.empty(),
                0, Duration.ofSeconds(5), Optional.empty(), 500);
        formattedMessage = testeeWithMessageLimit.formatMessage(record);

        assertEquals(500, formattedMessage.length());
        assertTrue(formattedMessage.endsWith(" (...)"));
    }

    @Test
    void shouldUseLogRecordTimestampNotSystemTime() throws Exception {
        // Setup mock CloudWatch client
        CloudWatchLogsClient mockClient = mock(CloudWatchLogsClient.class);
        PutLogEventsResponse mockResponse = PutLogEventsResponse.builder()
                .nextSequenceToken("next-token")
                .build();
        when(mockClient.putLogEvents(any(PutLogEventsRequest.class))).thenReturn(mockResponse);

        // Create handler with mock client
        LoggingCloudWatchHandler handler = new LoggingCloudWatchHandler(
                mockClient,
                "test-group",
                "test-stream",
                "initial-token",
                Optional.of(100),
                10,
                Duration.ofSeconds(1),
                Optional.empty(),
                0);
        handler.setLevel(Level.INFO);

        // Create log record with specific timestamp (1 hour ago)
        Instant specificTimestamp = Instant.now().minusSeconds(3600); // 1 hour ago
        LogRecord record = new LogRecord(Level.INFO, "Test message");
        record.setInstant(specificTimestamp);

        // Publish the record
        handler.publish(record);

        // Wait a bit and trigger flush by closing (which calls publisher.run())
        Thread.sleep(100);
        handler.close();

        // Verify that putLogEvents was called
        ArgumentCaptor<PutLogEventsRequest> requestCaptor = ArgumentCaptor.forClass(PutLogEventsRequest.class);
        verify(mockClient, atLeastOnce()).putLogEvents(requestCaptor.capture());

        // Get the events that were sent
        List<PutLogEventsRequest> requests = requestCaptor.getAllValues();
        assertFalse(requests.isEmpty(), "Expected at least one putLogEvents call");

        // Find the request with our event
        boolean foundEvent = false;
        for (PutLogEventsRequest request : requests) {
            if (!request.logEvents().isEmpty()) {
                InputLogEvent event = request.logEvents().get(0);
                // Verify the timestamp matches the LogRecord's timestamp, not current time
                assertEquals(specificTimestamp.toEpochMilli(), event.timestamp(),
                        "Event timestamp should match LogRecord.getInstant().toEpochMilli(), not System.currentTimeMillis()");
                foundEvent = true;
                break;
            }
        }
        assertTrue(foundEvent, "Expected to find the published event");
    }

    @Test
    void shouldSortEventsByTimestampBeforeSending() throws Exception {
        // Setup mock CloudWatch client
        CloudWatchLogsClient mockClient = mock(CloudWatchLogsClient.class);
        PutLogEventsResponse mockResponse = PutLogEventsResponse.builder()
                .nextSequenceToken("next-token")
                .build();
        when(mockClient.putLogEvents(any(PutLogEventsRequest.class))).thenReturn(mockResponse);

        // Create handler with mock client and small batch size
        LoggingCloudWatchHandler handler = new LoggingCloudWatchHandler(
                mockClient,
                "test-group",
                "test-stream",
                "initial-token",
                Optional.of(100),
                10,
                Duration.ofSeconds(1),
                Optional.empty(),
                0);
        handler.setLevel(Level.INFO);

        // Create log records with timestamps in reverse order
        Instant baseTime = Instant.now();
        LogRecord record1 = new LogRecord(Level.INFO, "Third message");
        record1.setInstant(baseTime.plusSeconds(2)); // Latest

        LogRecord record2 = new LogRecord(Level.INFO, "First message");
        record2.setInstant(baseTime); // Earliest

        LogRecord record3 = new LogRecord(Level.INFO, "Second message");
        record3.setInstant(baseTime.plusSeconds(1)); // Middle

        // Publish records in non-chronological order
        handler.publish(record1);
        handler.publish(record2);
        handler.publish(record3);

        // Wait a bit and trigger flush
        Thread.sleep(100);
        handler.close();

        // Verify that putLogEvents was called
        ArgumentCaptor<PutLogEventsRequest> requestCaptor = ArgumentCaptor.forClass(PutLogEventsRequest.class);
        verify(mockClient, atLeastOnce()).putLogEvents(requestCaptor.capture());

        // Get the events that were sent
        List<PutLogEventsRequest> requests = requestCaptor.getAllValues();
        assertFalse(requests.isEmpty(), "Expected at least one putLogEvents call");

        // Find the request with our events
        boolean foundEvents = false;
        for (PutLogEventsRequest request : requests) {
            if (request.logEvents().size() >= 3) {
                List<InputLogEvent> events = request.logEvents();
                // Verify events are sorted by timestamp in ascending order
                assertEquals(baseTime.toEpochMilli(), events.get(0).timestamp(),
                        "First event should have earliest timestamp");
                assertEquals(baseTime.plusSeconds(1).toEpochMilli(), events.get(1).timestamp(),
                        "Second event should have middle timestamp");
                assertEquals(baseTime.plusSeconds(2).toEpochMilli(), events.get(2).timestamp(),
                        "Third event should have latest timestamp");

                // Verify the messages are in the correct order after sorting
                assertTrue(events.get(0).message().contains("First message"));
                assertTrue(events.get(1).message().contains("Second message"));
                assertTrue(events.get(2).message().contains("Third message"));

                foundEvents = true;
                break;
            }
        }
        assertTrue(foundEvents, "Expected to find all three published events in sorted order");
    }
}
