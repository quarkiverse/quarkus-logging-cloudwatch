/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkiverse.logging.cloudwatch;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

/**
 * Configuration for CloudWatch logging.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME, name = "log.cloudwatch")
public class LoggingCloudWatchConfig {

    /**
     * Determine whether to enable the Cloudwatch logging extension.
     */
    @ConfigItem(defaultValue = "true")
    boolean enabled;

    /**
     * CW access key ID
     */
    @ConfigItem
    public Optional<String> accessKeyId;

    /**
     * CW access key secret
     */
    @ConfigItem
    public Optional<String> accessKeySecret;

    /**
     * Region of deployment
     */
    @ConfigItem
    public Optional<String> region;

    /**
     * CW log group
     */
    @ConfigItem
    public Optional<String> logGroup;

    /**
     * CW log stream
     */
    @ConfigItem
    public Optional<String> logStreamName;

    /**
     * The CW log level.
     */
    @ConfigItem(defaultValue = "WARN")
    public Level level;

    /**
     * Number of log events sent to CloudWatch per batch.
     * Defaults to 10,000 which is the maximum number of log events per batch allowed by CloudWatch.
     */
    @ConfigItem(defaultValue = "10000")
    public int batchSize;

    /**
     * Period between two batch executions.
     * Defaults to 5 seconds.
     */
    @ConfigItem(defaultValue = "5s")
    public Duration batchPeriod;

    /**
     * Maximum size of the log events queue.
     * If this is not set, the queue will have a capacity of {@link Integer#MAX_VALUE}.
     */
    @ConfigItem
    public Optional<Integer> maxQueueSize;

    /**
     * Service environment added as a {@code service.environment} field to each log record when available.
     */
    @ConfigItem
    public Optional<String> serviceEnvironment;

    /**
     * Amount of time to allow the CloudWatch client to complete the execution of an API call. This timeout covers the
     * entire client execution except for marshalling. This includes request handler execution, all HTTP requests
     * including retries, unmarshalling, etc. This value should always be positive, if present.
     */
    @ConfigItem
    public Optional<Duration> apiCallTimeout;

    /**
     * Default credentials provider enabled added as a {@code quarkus.log.cloudwatch.default-credentials-provider.enabled}
     */
    @ConfigItem(name = "quarkus.log.cloudwatch.default-credentials-provider.enabled", defaultValue = "false")
    public boolean defaultCredentialsProviderEnabled;

    /*
     * We need to validate that the values are present, even if marked as optional.
     * We need to mark them as optional, as otherwise the config would mark them
     * as bad even before the extension can check if the values are needed at all.
     */
    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (!defaultCredentialsProviderEnabled) {
            if (accessKeyId.isEmpty()) {
                errors.add("quarkus.log.cloudwatch.access-key-id");
            }
            if (accessKeySecret.isEmpty()) {
                errors.add("quarkus.log.cloudwatch.access-key-secret");
            }
        }
        if (region.isEmpty()) {
            errors.add("quarkus.log.cloudwatch.region");
        }
        if (logGroup.isEmpty()) {
            errors.add("quarkus.log.cloudwatch.log-group");
        }
        if (logStreamName.isEmpty()) {
            errors.add("quarkus.log.cloudwatch.log-stream-name");
        }

        return errors;
    }
}
