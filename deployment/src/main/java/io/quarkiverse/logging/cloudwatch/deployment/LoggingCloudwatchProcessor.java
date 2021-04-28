package io.quarkiverse.logging.cloudwatch.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

class LoggingCloudwatchProcessor {

    private static final String FEATURE = "logging-cloudwatch";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }
}
