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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

import com.amazonaws.services.logs.AWSLogs;
import com.amazonaws.services.logs.model.InputLogEvent;
import com.amazonaws.services.logs.model.InvalidSequenceTokenException;
import com.amazonaws.services.logs.model.PutLogEventsRequest;
import com.amazonaws.services.logs.model.PutLogEventsResult;

import io.quarkiverse.logging.cloudwatch.format.ElasticCommonSchemaLogFormatter;
import io.quarkus.logging.Log;

class LoggingCloudWatchHandler extends Handler {

    private AWSLogs awsLogs;
    private String logStreamName;
    private String logGroupName;
    private String sequenceToken;
    private int batchSize;
    private Optional<String> serviceEnvironment;

    private BlockingQueue<InputLogEvent> eventBuffer;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private Publisher publisher;

    LoggingCloudWatchHandler() {
    }

    LoggingCloudWatchHandler(AWSLogs awsLogs, String logGroup, String logStreamName, String token,
            Optional<Integer> maxQueueSize, int batchSize, Duration batchPeriod, Optional<String> serviceEnvironment) {
        this.logGroupName = logGroup;
        this.awsLogs = awsLogs;
        this.logStreamName = logStreamName;
        this.sequenceToken = token;
        if (maxQueueSize.isPresent()) {
            eventBuffer = new LinkedBlockingQueue<>(maxQueueSize.get());
        } else {
            eventBuffer = new LinkedBlockingQueue<>();
        }
        this.batchSize = batchSize;
        this.serviceEnvironment = serviceEnvironment;

        this.publisher = new Publisher();
        scheduler.scheduleAtFixedRate(publisher, 5, batchPeriod.toMillis(), TimeUnit.MILLISECONDS);
    }

    @Override
    public void publish(LogRecord record) {
        if (isBelowThreshold(record)) {
            return;
        }

        record.setMessage(String.format(record.getMessage(), record.getParameters()));
        ElasticCommonSchemaLogFormatter formatter = new ElasticCommonSchemaLogFormatter(serviceEnvironment);
        String body = formatter.format(record);

        InputLogEvent logEvent = new InputLogEvent()
                .withMessage(body)
                .withTimestamp(System.currentTimeMillis());

        // Queue this up, so that it can be flushed later in batch asynchronously
        boolean inserted = eventBuffer.offer(logEvent);
        if (!inserted) {
            Log.warn(
                    "Maximum size of the CloudWatch log events queue reached. Consider increasing that size from the configuration.");
        }
    }

    /**
     * Skip messages that are below the configured threshold.
     */
    boolean isBelowThreshold(LogRecord record) {
        return record.getLevel().intValue() < getLevel().intValue();
    }

    @Override
    public void flush() {
    }

    @Override
    public void close() throws SecurityException {
        Log.info("Shutting down and awaiting termination");
        shutdownAndAwaitTermination(scheduler);

        Log.info("Trying to send of last log messages after shutdown.");
        publisher.run();
    }

    private class Publisher implements Runnable {

        @Override
        public void run() {
            List<InputLogEvent> events = new ArrayList<>(Math.min(eventBuffer.size(), batchSize));
            eventBuffer.drainTo(events, batchSize);
            boolean workingSequenceToken = false;
            if (events.size() > 0) {
                PutLogEventsRequest request = new PutLogEventsRequest();
                request.setLogEvents(events);
                request.setLogGroupName(logGroupName);
                request.setLogStreamName(logStreamName);

                int counter = 0;
                while (!workingSequenceToken && counter < 10) {
                    request.setSequenceToken(sequenceToken);

                    try {
                        /*
                         * The following call never ends when the batch size is higher than 1,048,576 bytes
                         * which is the maximum size allowed by CloudWatch as explained in their Javadoc.
                         * See https://github.com/aws/aws-sdk-java/issues/2807
                         */
                        PutLogEventsResult result = awsLogs.putLogEvents(request);
                        sequenceToken = result.getNextSequenceToken();
                        workingSequenceToken = true;
                        counter = 10;
                    } catch (InvalidSequenceTokenException e) {
                        String exceptionMessage = e.getMessage();
                        Log.infof("exception message: %s", exceptionMessage);

                        sequenceToken = extractValidSequenceToken(exceptionMessage);
                        Log.infof("extracted sequence token: %s", sequenceToken);

                        workingSequenceToken = false;
                    }
                    counter = checkAndIncreaseCounter(counter);
                }
            }
        }

        private int checkAndIncreaseCounter(int counter) {
            if (counter == 9) {
                Log.error("Last counter iteration now. Too many attempts. Will abort trying now.");
            }
            counter++;
            return counter;
        }
    }

    String extractValidSequenceToken(String exceptionMessage) {
        return exceptionMessage.substring(exceptionMessage.indexOf(":") + 1, exceptionMessage.indexOf("(")).trim();
    }

    private void shutdownAndAwaitTermination(ExecutorService pool) {
        pool.shutdown(); // Disable new tasks from being submitted
        try {
            // Wait a while for existing tasks to terminate
            if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                pool.shutdownNow(); // Cancel currently executing tasks
                // Wait a while for tasks to respond to being cancelled
                if (!pool.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException ie) {
            // (Re-)Cancel if current thread also interrupted
            pool.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
