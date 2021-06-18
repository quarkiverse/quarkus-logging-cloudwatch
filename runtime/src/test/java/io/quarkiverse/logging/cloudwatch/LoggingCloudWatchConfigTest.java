package io.quarkiverse.logging.cloudwatch;

import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoggingCloudWatchConfigTest {

    private final LoggingCloudWatchConfig testee = new LoggingCloudWatchConfig();

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
        testee.accessKeyId = Optional.ofNullable(null);
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> testee.validate()
        );

        assertEquals("Access key id not provided", thrown.getMessage());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenAccessKeySecretIsNotPresent() {
        testee.accessKeyId = Optional.of("someAccessKeyId");
        testee.accessKeySecret = Optional.ofNullable(null);
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> testee.validate()
        );

        assertEquals("Access key secret not provided", thrown.getMessage());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenRegionIsNotPresent() {
        testee.accessKeyId = Optional.of("someAccessKeyId");
        testee.accessKeySecret = Optional.of("someAccessKeySecret");
        testee.region = Optional.ofNullable(null);
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> testee.validate()
        );

        assertEquals("Region not provided", thrown.getMessage());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenLogGroupIsNotPresent() {
        testee.accessKeyId = Optional.of("someAccessKeyId");
        testee.accessKeySecret = Optional.of("someAccessKeySecret");
        testee.region = Optional.of("someRegion");
        testee.logGroup = Optional.ofNullable(null);
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> testee.validate()
        );

        assertEquals("Log group not provided", thrown.getMessage());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenLogStreamNameIsNotPresent() {
        testee.accessKeyId = Optional.of("someAccessKeyId");
        testee.accessKeySecret = Optional.of("someAccessKeySecret");
        testee.region = Optional.of("someRegion");
        testee.logGroup = Optional.of("someLogGroup");
        testee.logStreamName = Optional.ofNullable(null);
        IllegalStateException thrown = assertThrows(
                IllegalStateException.class,
                () -> testee.validate()
        );

        assertEquals("Log stream not provided", thrown.getMessage());
    }
}