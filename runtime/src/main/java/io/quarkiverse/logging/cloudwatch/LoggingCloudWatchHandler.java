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

import static java.util.stream.Collectors.joining;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.jboss.logmanager.ExtLogRecord;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.InvalidSequenceTokenException;
import com.amazonaws.services.logs.model.PutLogEventsRequest;

public class LoggingCloudWatchHandler extends Handler {

    private String appLabel;
    private final AWSLogs awsLogs;
    private final String logStreamName;
    private final String logGroupName;
    private String sequenceToken;
    private volatile boolean done = false;

    final List<InputLogEvent> eventBuffer;

    public LoggingCloudWatchHandler(AWSLogs awsLogs, String logGroup, String logStreamName, String token) {
        this.logGroupName = logGroup;
        this.awsLogs = awsLogs;
        this.logStreamName = logStreamName;
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

        Map<String, String> tags = new HashMap<>();

        String host = record instanceof ExtLogRecord ? ((ExtLogRecord) record).getHostName() : null;
        if (record.getLoggerName().equals("__AccessLog")) {
            tags.put("type", "access");
        }
        if (host != null && !host.isEmpty()) {
            tags.put("host", host);
        }
        if (appLabel != null && !appLabel.isEmpty()) {
            tags.put("app", appLabel);
        }

        tags.put("level", record.getLevel().getName());

        String msg;
        if (record.getParameters() != null && record.getParameters().length > 0) {
            switch (((ExtLogRecord) record).getFormatStyle()) {
                case PRINTF:
                    msg = String.format(record.getMessage(), record.getParameters());
                    break;
                case MESSAGE_FORMAT:
                    msg = MessageFormat.format(record.getMessage(), record.getParameters());
                    break;
                default: // == NO_FORMAT
                    msg = record.getMessage();
            }
        } else {
            msg = record.getMessage();
        }

        if (record instanceof ExtLogRecord) {

            String tid = ((ExtLogRecord) record).getMdc("traceId");
            if (tid != null) {
                tags.put("traceId", tid);
            }
        }

        String body = assemblePayload(msg, tags, record.getThrown());

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

    private String assemblePayload(String message, Map<String, String> tags, Throwable thrown) {

        StringBuilder sb = new StringBuilder();
        sb.append("msg=[").append(message).append("]");
        if (thrown != null) {
            sb.append(", stacktrace=[");
            fillStackTrace(sb, thrown);
            sb.append("]");
        }
        if (!tags.isEmpty()) {
            sb.append(", tags=[");
            String tagsAsString = tags.keySet().stream()
                    .map(key -> key + "=" + tags.get(key))
                    .collect(joining(", "));
            sb.append(tagsAsString);
            sb.append("]");
        }
        return sb.toString();
    }

    private void fillStackTrace(StringBuilder sb, Throwable thrown) {
        for (StackTraceElement ste : thrown.getStackTrace()) {
            sb.append("  ").append(ste.toString()).append("\n");
        }
        if (thrown.getCause() != null) {
            sb.append("Caused by:");
            fillStackTrace(sb, thrown.getCause());
        }
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

                        String validSequenceToken = extractValidSequenceToken(exceptionMessage);

                        request.setSequenceToken(validSequenceToken);
                        sequenceToken = awsLogs.putLogEvents(request).getNextSequenceToken();
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
