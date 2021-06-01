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

import java.util.ArrayList;
import java.util.List;
import java.util.logging.*;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.InvalidSequenceTokenException;
import com.amazonaws.services.logs.model.PutLogEventsRequest;

public class LoggingCloudWatchHandler extends Handler {

    Logger log = Logger.getLogger("LoggingCloudWatch");

    private String appLabel;
    private final AWSLogs awsLogs;
    private final String logStreamName;
    private final String logGroupName;
    private String sequenceToken;
    private volatile boolean done = false;

    final List<InputLogEvent> eventBuffer;

    private LoggingCloudWatchConfig config;

    public LoggingCloudWatchHandler(AWSLogs awsLogs, String logGroup, String logStreamName, String token,
            LoggingCloudWatchConfig config) {
        this.logGroupName = logGroup;
        this.awsLogs = awsLogs;
        this.logStreamName = logStreamName;
        this.config = config;
        sequenceToken = token;
        eventBuffer = new ArrayList<>();
        Publisher publisher = new Publisher();

        Thread t = new Thread(publisher);
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void publish(LogRecord record) {
        // Skip messages that are below the configured threshold
        if (record.getLevel().intValue() < getLevel().intValue()) {
            return;
        }

        ElasticCommonSchemaLogFormatter elasticCommonSchemaLogFormatter = new ElasticCommonSchemaLogFormatter(config);
        String body = elasticCommonSchemaLogFormatter.format(record);

        InputLogEvent logEvent = new InputLogEvent()
                .withMessage(body)
                .withTimestamp(System.currentTimeMillis());
        // Queue this up, so that it can be flushed later in batch
        // Asynchronously
        eventBuffer.add(logEvent);
    }

    @Override
    public void flush() {
        done = true;
    }

    @Override
    public void close() throws SecurityException {
    }

    public void setAppLabel(String label) {
        if (label != null) {
            this.appLabel = label;
        }
    }

    private class Publisher implements Runnable {

        List<InputLogEvent> events;

        @Override
        public void run() {
            while (true) {
                synchronized (eventBuffer) {
                    events = new ArrayList<>(eventBuffer);
                    eventBuffer.clear();
                }

                if (events.size() > 0) {
                    PutLogEventsRequest request = new PutLogEventsRequest();
                    request.setLogEvents(events);
                    request.setLogGroupName(logGroupName);
                    request.setLogStreamName(logStreamName);
                    request.setSequenceToken(sequenceToken);
                    // Do the call and get the next token

                    try {
                        sequenceToken = awsLogs.putLogEvents(request).getNextSequenceToken();
                    } catch (InvalidSequenceTokenException e) {
                        String exceptionMessage = e.getMessage();
                        log.info("exception message: " + exceptionMessage);
                        String validSequenceToken = extractValidSequenceToken(exceptionMessage);
                        sequenceToken = validSequenceToken;
                        log.info("valid sequence token: " + validSequenceToken);
                        log.info("actual sequence token: " + sequenceToken);

                        PutLogEventsRequest newRequest = new PutLogEventsRequest();
                        newRequest.setLogEvents(events);
                        newRequest.setLogGroupName(logGroupName);
                        newRequest.setLogStreamName(logStreamName);
                        newRequest.setSequenceToken(validSequenceToken);

                        sequenceToken = awsLogs.putLogEvents(newRequest).getNextSequenceToken();
                    }
                }

                if (done) {
                    return;
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace(); // TODO: Customise this generated block
                }
            }
        }

        private String extractValidSequenceToken(String exceptionMessage) {
            return exceptionMessage.substring(exceptionMessage.indexOf(":") + 1, exceptionMessage.indexOf("("));
        }
    }
}
