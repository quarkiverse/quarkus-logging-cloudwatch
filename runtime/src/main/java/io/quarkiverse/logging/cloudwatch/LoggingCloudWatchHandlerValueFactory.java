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

import static io.quarkus.runtime.LaunchMode.DEVELOPMENT;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;

import org.jboss.logging.Logger;

import io.quarkiverse.logging.cloudwatch.auth.CloudWatchCredentialsProvider;
import io.quarkus.runtime.LaunchMode;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;
import software.amazon.awssdk.core.client.config.ClientOverrideConfiguration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClientBuilder;
import software.amazon.awssdk.services.cloudwatchlogs.model.CreateLogStreamRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.DescribeLogStreamsRequest;
import software.amazon.awssdk.services.cloudwatchlogs.model.LogStream;

@Recorder
public class LoggingCloudWatchHandlerValueFactory {

    private static final Logger LOGGER = Logger.getLogger(LoggingCloudWatchHandlerValueFactory.class);

    public RuntimeValue<Optional<Handler>> create(final LoggingCloudWatchConfig config) {
        if (!config.enabled()) {
            LOGGER.info("Quarkus Logging Cloudwatch Extension is not enabled");
            return new RuntimeValue<>(Optional.empty());
        }

        LOGGER.info("Initializing Quarkus Logging Cloudwatch Extension");

        List<String> errors = config.validate();
        if (!errors.isEmpty()) {
            String errorMsg = "The Quarkus Logging Cloudwatch extension is unable to start because of missing configuration values: "
                    + String.join(", ", errors);
            if (LaunchMode.current() == DEVELOPMENT) {
                LOGGER.error(errorMsg);
                return new RuntimeValue<>(Optional.empty());
            } else {
                throw new IllegalStateException(errorMsg);
            }
        }

        LOGGER.infof("Logging to log-group: %s and log-stream: %s", config.logGroup().get(), config.logStreamName().get());

        CloudWatchLogsClientBuilder cloudWatchLogsClientBuilder = CloudWatchLogsClient.builder()
                .credentialsProvider(new CloudWatchCredentialsProvider(config))
                .region(Region.of(config.region().get()));
        if (config.apiCallTimeout().isPresent()) {
            cloudWatchLogsClientBuilder = cloudWatchLogsClientBuilder.overrideConfiguration(
                    ClientOverrideConfiguration.builder().apiCallTimeout(config.apiCallTimeout().get()).build());
        }
        if (config.endpointOverride().isPresent()) {
            cloudWatchLogsClientBuilder.endpointOverride(URI.create(config.endpointOverride().get()));
        }

        CloudWatchLogsClient cloudWatchLogsClient = cloudWatchLogsClientBuilder.build();

        String token = createLogStreamIfNeeded(cloudWatchLogsClient, config);

        LoggingCloudWatchHandler handler = new LoggingCloudWatchHandler(cloudWatchLogsClient, config.logGroup().get(),
                config.logStreamName().get(), token, config.maxQueueSize(), config.batchSize(), config.batchPeriod(),
                config.serviceEnvironment());
        handler.setLevel(config.level());

        return new RuntimeValue<>(Optional.of(handler));
    }

    private String createLogStreamIfNeeded(CloudWatchLogsClient cloudWatchLogsClient, LoggingCloudWatchConfig config) {
        String token = null;

        DescribeLogStreamsRequest describeLogStreamsRequest = DescribeLogStreamsRequest.builder()
                .logGroupName(config.logGroup().get())
                // We need to filter down, as CW returns by default only 50 streams and ours may not be in it.
                .logStreamNamePrefix(config.logStreamName().get())
                .build();
        List<LogStream> logStreams = cloudWatchLogsClient.describeLogStreams(describeLogStreamsRequest).logStreams();

        boolean found = false;
        for (LogStream ls : logStreams) {
            if (ls.logStreamName().equals(config.logStreamName().get())) {
                found = true;
                token = ls.uploadSequenceToken();
            }
        }

        if (!found) {
            CreateLogStreamRequest createLogStreamRequest = CreateLogStreamRequest.builder()
                    .logGroupName(config.logGroup().get())
                    .logStreamName(config.logStreamName().get())
                    .build();
            cloudWatchLogsClient.createLogStream(createLogStreamRequest);
        }
        return token;
    }

}
