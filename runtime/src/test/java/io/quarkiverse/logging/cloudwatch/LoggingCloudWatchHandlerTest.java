package io.quarkiverse.logging.cloudwatch;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class LoggingCloudWatchHandlerTest {

    private LoggingCloudWatchHandler handler;

    LoggingCloudWatchHandlerTest() {
        this.handler = new LoggingCloudWatchHandler();
    }

    @Test
    void shouldExtractNextSequenceToken() {
        Exception e = new RuntimeException(
                "The given sequenceToken is invalid. The next expected sequenceToken is: 49611889230645343415657219696041834137426496333583221330 (Service: AWSLogs; Status Code: 400; Error Code: InvalidSequenceTokenException; Request ID: 02120a3d-bc00-45df-96fd-b3491ba01924; Proxy: null)");
        String exceptionMessage = e.getMessage();
        final String actual = handler.extractValidSequenceToken(exceptionMessage);

        assertEquals("49611889230645343415657219696041834137426496333583221330", actual);
    }
}
