package io.quarkiverse.logging.cloudwatch.deployment;

import io.quarkiverse.logging.cloudwatch.LoggingCloudWatchHandlerValueFactory;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.LogHandlerBuildItem;

class LoggingCloudwatchProcessor {

    private static final String FEATURE = "logging-cloudwatch";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    LogHandlerBuildItem addCloudwatchLogHandler(
            final LoggingCloudWatchHandlerValueFactory cloudWatchHandlerValueFactory) {
        return new LogHandlerBuildItem(cloudWatchHandlerValueFactory.create());
    }
}
