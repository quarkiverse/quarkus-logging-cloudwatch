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

import java.util.List;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Logger;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.AWSLogsClientBuilder;
import com.amazonaws.services.logs.model.CreateLogStreamRequest;
import com.amazonaws.services.logs.model.DescribeLogStreamsRequest;
import com.amazonaws.services.logs.model.LogStream;

import io.quarkiverse.logging.cloudwatch.auth.CWCredentialsProvider;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class LoggingCloudWatchHandlerValueFactory {

    private static final Logger LOGGER = Logger.getLogger("LoggingCloudWatch");

    public RuntimeValue<Optional<Handler>> create(final LoggingCloudWatchConfig config) {
        if (!config.enabled) {
            LOGGER.fine("Quarkus Logging Cloudwatch Extension is not enabled");
            return new RuntimeValue<>(Optional.empty());
        }

        config.validate();

        LOGGER.info("Initializing Quarkus Logging Cloudwatch Extension");
        LOGGER.info("Logging to log-group: " + config.logGroup + " and log-stream: " + config.logStreamName);

        AWSLogsClientBuilder clientBuilder = AWSLogsClientBuilder.standard();
        clientBuilder.setCredentials(new CWCredentialsProvider(config));
        clientBuilder.setRegion(config.region.get());

        AWSLogs awsLogs = clientBuilder.build();
        String token = createLogStreamIfNeeded(awsLogs, config);

        LoggingCloudWatchHandler handler = new LoggingCloudWatchHandler(awsLogs, config.logGroup.get(),
                config.logStreamName.get(), token);
        handler.setLevel(config.level);

        return new RuntimeValue<>(Optional.of(handler));
    }

    private String createLogStreamIfNeeded(AWSLogs awsLogs, LoggingCloudWatchConfig config) {
        String token = null;

        DescribeLogStreamsRequest describeLogStreamsRequest = new DescribeLogStreamsRequest(config.logGroup.get());
        // We need to filter down, as CW returns by default only 50 streams and ours may not be in it.
        describeLogStreamsRequest.withLogStreamNamePrefix(config.logStreamName.get());
        List<LogStream> logStreams = awsLogs.describeLogStreams(describeLogStreamsRequest).getLogStreams();

        boolean found = false;
        for (LogStream ls : logStreams) {
            if (ls.getLogStreamName().equals(config.logStreamName.get())) {
                found = true;
                token = ls.getUploadSequenceToken();
            }
        }

        if (!found) {
            awsLogs.createLogStream(new CreateLogStreamRequest(config.logGroup.get(), config.logStreamName.get()));
        }
        return token;
    }

}
