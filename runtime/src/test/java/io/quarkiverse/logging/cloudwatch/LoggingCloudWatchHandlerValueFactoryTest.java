package io.quarkiverse.logging.cloudwatch;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.logging.Handler;

import org.junit.jupiter.api.Test;

import io.quarkus.runtime.RuntimeValue;

class LoggingCloudWatchHandlerValueFactoryTest {

    private final LoggingCloudWatchHandlerValueFactory testee = new LoggingCloudWatchHandlerValueFactory();

    @Test
    void shouldReturnEmptyRuntimeValueWhenConfigIsEmptyAndHasEnabledFalseByDefault() {
        LoggingCloudWatchConfig config = new LoggingCloudWatchConfig();

        final RuntimeValue<Optional<Handler>> actualRuntimeValue = testee.create(config);

        assertFalse(actualRuntimeValue.getValue().isPresent());
    }

    @Test
    void shouldReturnEmptyRuneTimeValueWhenConfigIsNotEnabled() {
        LoggingCloudWatchConfig config = new LoggingCloudWatchConfig();
        config.enabled = false;

        final RuntimeValue<Optional<Handler>> actualRuntimeValue = testee.create(config);

        assertFalse(actualRuntimeValue.getValue().isPresent());
    }
}