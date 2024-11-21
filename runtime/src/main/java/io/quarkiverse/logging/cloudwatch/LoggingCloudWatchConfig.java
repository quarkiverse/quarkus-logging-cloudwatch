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

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

/**
 * Configuration for CloudWatch logging.
 */
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
@ConfigMapping(prefix = "quarkus.log.cloudwatch")
public interface LoggingCloudWatchConfig {

    /**
     * Determine whether to enable the Cloudwatch logging extension.
     */
    @WithName("enabled")
    @WithDefault("true")
    boolean enabled();

    /**
     * The AWS CloudWatch access key id
     */
    @WithName("access-key-id")
    Optional<String> accessKeyId();

    /**
     * The AWS CloudWatch access key secret
     */
    @WithName("access-key-secret")
    Optional<String> accessKeySecret();

    /**
     * Region of deployment
     */
    @WithName("region")
    Optional<String> region();

    /**
     * CW log group
     */
    @WithName("log-group")
    Optional<String> logGroup();

    /**
     * CW log stream
     */
    @WithName("log-stream-name")
    Optional<String> logStreamName();

    /**
     * The CW log level.
     */
    @WithName("level")
    @WithDefault("WARN")
    Level level();

    /**
     * Number of log events sent to CloudWatch per batch.
     * Defaults to 10,000 which is the maximum number of log events per batch allowed by CloudWatch.
     */
    @WithDefault("10000")
    int batchSize();

    /**
     * Period between two batch executions.
     * Defaults to 5 seconds.
     */
    @WithDefault("5s")
    Duration batchPeriod();

    /**
     * Maximum size of the log events queue.
     * If this is not set, the queue will have a capacity of {@link Integer#MAX_VALUE}.
     */
    @WithName("max-queue-size")
    Optional<Integer> maxQueueSize();

    /**
     * Service environment added as a {@code service.environment} field to each log record when available.
     */
    @WithName("service-environment")
    Optional<String> serviceEnvironment();

    /**
     * Amount of time to allow the CloudWatch client to complete the execution of an API call. This timeout covers the
     * entire client execution except for marshalling. This includes request handler execution, all HTTP requests
     * including retries, unmarshalling, etc. This value should always be positive, if present.
     */
    @WithName("api-call-timeout")
    Optional<Duration> apiCallTimeout();

    /**
     * Default credentials provider enabled added as a {@code quarkus.log.cloudwatch.default-credentials-provider.enabled}
     */
    @WithName("default-credentials-provider.enabled")
    @WithDefault("false")
    boolean defaultCredentialsProviderEnabled();

    /**
     * Endpoint override added as {@code endpoint-override}
     */
    @WithName("endpoint-override")
    Optional<String> endpointOverride();

    /*
     * We need to validate that the values are present, even if marked as optional.
     * We need to mark them as optional, as otherwise the config would mark them
     * as bad even before the extension can check if the values are needed at all.
     */
    default List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (!defaultCredentialsProviderEnabled()) {
            if (accessKeyId().isEmpty()) {
                errors.add("quarkus.log.cloudwatch.access-key-id");
            }
            if (accessKeySecret().isEmpty()) {
                errors.add("quarkus.log.cloudwatch.access-key-secret");
            }
        }
        if (region().isEmpty()) {
            errors.add("quarkus.log.cloudwatch.region");
        }
        if (logGroup().isEmpty()) {
            errors.add("quarkus.log.cloudwatch.log-group");
        }
        if (logStreamName().isEmpty()) {
            errors.add("quarkus.log.cloudwatch.log-stream-name");
        }
        return errors;
    }
}
