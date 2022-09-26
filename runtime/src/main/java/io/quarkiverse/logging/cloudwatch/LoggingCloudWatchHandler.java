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

import io.quarkiverse.logging.cloudwatch.format.ElasticCommonSchemaLogFormatter;
import io.quarkus.logging.Log;
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient;
import software.amazon.awssdk.services.cloudwatchlogs.model.InputLogEvent;
import software.amazon.awssdk.services.cloudwatchlogs.model.InvalidSequenceTokenException;
import software.amazon.awssdk.services.cloudwatchlogs.model.PutLogEventsRequest;

class LoggingCloudWatchHandler extends Handler {

    private static final int BATCH_MAX_ATTEMPTS = 10;

    private CloudWatchLogsClient cloudWatchLogsClient;
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

    LoggingCloudWatchHandler(CloudWatchLogsClient cloudWatchLogsClient, String logGroup, String logStreamName, String token,
            Optional<Integer> maxQueueSize, int batchSize, Duration batchPeriod, Optional<String> serviceEnvironment) {
        this.logGroupName = logGroup;
        this.cloudWatchLogsClient = cloudWatchLogsClient;
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

        InputLogEvent logEvent = InputLogEvent.builder()
                .message(body)
                .timestamp(System.currentTimeMillis())
                .build();

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

            // First, let's poll from the queue the events that will be part of the batch.
            List<InputLogEvent> events = new ArrayList<>(Math.min(eventBuffer.size(), batchSize));
            eventBuffer.drainTo(events, batchSize);
            if (events.size() > 0) {

                // The sequence token needed for this request is set below.
                PutLogEventsRequest request = PutLogEventsRequest.builder()
                        .logGroupName(logGroupName)
                        .logStreamName(logStreamName)
                        .logEvents(events)
                        .build();

                /*
                 * The current sequence token may not be valid if it was used by another application or pod.
                 * If that happens, we'll retry using the token from the InvalidSequenceTokenException.
                 */
                for (int i = 1; i <= BATCH_MAX_ATTEMPTS; i++) {

                    request = request.toBuilder()
                            .sequenceToken(sequenceToken)
                            .build();

                    try {
                        /*
                         * It's time to put the log events into CloudWatch.
                         * If that works, we'll use the sequence token from the response for the next put call.
                         */
                        sequenceToken = cloudWatchLogsClient.putLogEvents(request).nextSequenceToken();
                        // The sequence token was accepted, we don't need to retry.
                        break;
                    } catch (InvalidSequenceTokenException e) {
                        Log.debugf("PutLogEvents call failed because of an invalid sequence token", e);

                        // We'll use the sequence token from the exception for the next put call.
                        sequenceToken = e.expectedSequenceToken();

                        // If the last attempt failed, the log events from the current batch are lost.
                        if (i == BATCH_MAX_ATTEMPTS) {
                            Log.warn(
                                    "Too many retries for a PutLogEvents call, log events from the current batch will not be sent to CloudWatch");
                        }
                    }
                }
            }
        }
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
