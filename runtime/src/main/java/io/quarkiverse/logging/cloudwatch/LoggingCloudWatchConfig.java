/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
     *
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
     *
     */
    @ConfigItem
    public Optional<String> logStreamName;

    /**
     * The CW log level.
     */
    @ConfigItem(defaultValue = "WARN")
    public Level level;

    /*
     * We need to validate that the values are present, even if marked as optional.
     * We need to mark them as optional, as otherwise the config would mark them
     * as bad even before the extension can check if the values are needed at all.
     */
    public void validate() {

        if (!accessKeyId.isPresent()) {
            throw new IllegalStateException("Access key id not provided");
        }
        if (!accessKeySecret.isPresent()) {
            throw new IllegalStateException("Access key secret not provided");
        }
        if (!region.isPresent()) {
            throw new IllegalStateException("Region not provided");
        }
        if (!logGroup.isPresent()) {
            throw new IllegalStateException("Log group not provided");
        }
        if (!logStreamName.isPresent()) {
            throw new IllegalStateException("Log stream not provided");
        }
    }
}
