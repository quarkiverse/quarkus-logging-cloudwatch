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
    public String accessKeyId;

    /**
     * CW access key secret
     */
    @ConfigItem
    public String accessKeySecret;

    /**
     * Region of deployment
     */
    @ConfigItem
    public String region;

    /**
     * CW log group
     */
    @ConfigItem
    public String logGroup;

    /**
     * CW log stream
     */
    @ConfigItem
    public String logStreamName;

    /**
     * The CW log level.
     */
    @ConfigItem(defaultValue = "WARN")
    public Level level;
}
