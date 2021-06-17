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

    private static final Logger LOGGER = Logger.getLogger("LoggingCloudWatch");

    private AWSLogs awsLogs;
    private String logStreamName;
    private String logGroupName;
    private String sequenceToken;

    private volatile boolean done = false;

    private List<InputLogEvent> eventBuffer;

    public LoggingCloudWatchHandler() {
    }

    public LoggingCloudWatchHandler(AWSLogs awsLogs, String logGroup, String logStreamName, String token) {
        this.logGroupName = logGroup;
        this.awsLogs = awsLogs;
        this.logStreamName = logStreamName;
        this.sequenceToken = token;
        this.eventBuffer = new ArrayList<>();

        Publisher publisher = new Publisher();
        Thread t = new Thread(publisher);
        t.setDaemon(true);
        t.start();
    }

    @Override
    public void publish(LogRecord record) {
        if (isBelowThreshold(record)) {
            return;
        }

        if (record.getMessage().equals("test warning")) {
            System.out.println("test warning cloudwatch");
        }

        ElasticCommonSchemaLogFormatter formatter = new ElasticCommonSchemaLogFormatter();
        String body = formatter.format(record);

        InputLogEvent logEvent = new InputLogEvent()
                .withMessage(body)
                .withTimestamp(System.currentTimeMillis());

        // Queue this up, so that it can be flushed later in batch asynchronously
        eventBuffer.add(logEvent);
    }

    /**
     * Skip messages that are below the configured threshold.
     */
    private boolean isBelowThreshold(LogRecord record) {
        return record.getLevel().intValue() < getLevel().intValue();
    }

    @Override
    public void flush() {
        done = true;
    }

    @Override
    public void close() throws SecurityException {
    }

    private class Publisher implements Runnable {

        private List<InputLogEvent> events;

        @Override
        public void run() {
            while (true) {
                synchronized (eventBuffer) {
                    events = new ArrayList<>(eventBuffer);
                    eventBuffer.clear();
                }
                boolean workingSequenceToken = false;
                if (events.size() > 0) {
                    while (!workingSequenceToken) {
                        PutLogEventsRequest request = new PutLogEventsRequest();
                        request.setLogEvents(events);
                        request.setLogGroupName(logGroupName);
                        request.setLogStreamName(logStreamName);
                        request.setSequenceToken(sequenceToken);
                        // Do the call and get the next token

                        try {
                            sequenceToken = awsLogs.putLogEvents(request).getNextSequenceToken();
                            workingSequenceToken = true;
                        } catch (InvalidSequenceTokenException e) {
                            String exceptionMessage = e.getMessage();
                            LOGGER.info("exception message: " + exceptionMessage);
                            sequenceToken = extractValidSequenceToken(exceptionMessage);
                            LOGGER.info("extracted sequence token: " + sequenceToken);
                            workingSequenceToken = false;
                        }
                    }
                }

                if (done) {
                    return;
                }

                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    String extractValidSequenceToken(String exceptionMessage) {
        return exceptionMessage.substring(exceptionMessage.indexOf(":") + 1, exceptionMessage.indexOf("(")).trim();
    }
}
