package io.quarkiverse.logging.cloudwatch;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class LoggingCloudWatchConfigTest {

    private final LoggingCloudWatchConfig testee = createPreFilledTestConfig();

    @Test
    void shouldNotThrowIllegalStateExceptionWhenEveryAttributeIsPresent() {
        testee.accessKeyId = Optional.of("someAccessKeyId");
        testee.accessKeySecret = Optional.of("someAccessKeySecret");
        testee.region = Optional.of("someRegion");
        testee.logGroup = Optional.of("someLogGroup");
        testee.logStreamName = Optional.of("someLogStreamName");

        testee.validate();
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenAccessKeyIdIsNotPresent() {
        testee.accessKeyId = Optional.empty();

        List<String> errors = testee.validate();

        assertEquals(1, errors.size());
        assertEquals("quarkus.log.cloudwatch.access-key-id", errors.get(0), "Access key id not provided");
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenAccessKeySecretIsNotPresent() {
        testee.accessKeySecret = Optional.empty();

        List<String> errors = testee.validate();

        assertEquals(1, errors.size());
        assertEquals("quarkus.log.cloudwatch.access-key-secret", errors.get(0), "Access key secret not provided");
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenRegionIsNotPresent() {
        testee.region = Optional.empty();

        List<String> errors = testee.validate();

        assertEquals(1, errors.size());
        assertEquals("quarkus.log.cloudwatch.region", errors.get(0), "Region not provided");

    }

    @Test
    void shouldThrowIllegalStateExceptionWhenLogGroupIsNotPresent() {
        testee.logGroup = Optional.empty();

        List<String> errors = testee.validate();

        assertEquals(1, errors.size());
        assertEquals("quarkus.log.cloudwatch.log-group", errors.get(0), "Log group not provided");
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenLogStreamNameIsNotPresent() {
        testee.logStreamName = Optional.empty();

        List<String> errors = testee.validate();

        assertEquals(1, errors.size());
        assertEquals("quarkus.log.cloudwatch.log-stream-name", errors.get(0), "Log stream not provided");
    }

    @Test
    void shouldCollctErrorsWhenMultiplePropertiesAreNotProvided() {
        testee.logStreamName = Optional.empty();
        testee.region = Optional.empty();

        List<String> errors = testee.validate();

        assertEquals(2, errors.size());
        assertEquals("quarkus.log.cloudwatch.region", errors.get(0), "Region not provided");
        assertEquals("quarkus.log.cloudwatch.log-stream-name", errors.get(1), "Log stream not provided");
    }

    private LoggingCloudWatchConfig createPreFilledTestConfig() {
        LoggingCloudWatchConfig config = new LoggingCloudWatchConfig();
        config.accessKeyId = Optional.of("some-access-key-id");
        config.accessKeySecret = Optional.of("some-access-key-secret");
        config.region = Optional.of("some-region");
        config.logGroup = Optional.of("some-log-group");
        config.logStreamName = Optional.of("some-log-stream-name");
        return config;
    }
}
