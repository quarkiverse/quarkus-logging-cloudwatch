package io.quarkiverse.logging.cloudwatch.it;

import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.CLOUDWATCH;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class LoggingCloudwatchResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingCloudwatchResource.class);

    private final LocalStackContainer localstack;

    public LoggingCloudwatchResource() {
        DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");
        this.localstack = new LocalStackContainer(localstackImage).withServices(S3, CLOUDWATCH);
    }

    @Test
    public void someTestMethod() {
        try {
            localstack.start();
            AmazonS3 s3 = AmazonS3ClientBuilder
                    .standard()
                    .withEndpointConfiguration(localstack.getEndpointConfiguration(S3))
                    .withCredentials(localstack.getDefaultCredentialsProvider())
                    .build();

            s3.createBucket("foo");
            s3.putObject("foo", "bar", "baz");
            final S3Object s3Object = s3.getObject("foo", "bar");

            assertEquals("foo", s3Object.getBucketName());
            assertEquals("bar", s3Object.getKey());
        } finally {
            localstack.close();
        }
    }

    @Override
    public Map<String, String> start() {
        LOGGER.info("Starting localstack CloudWatch docker container");
        localstack.start();
        LOGGER.info("CloudWatch docker container started");
        return new HashMap<>();
    }

    @Override
    public void stop() {
        localstack.stop();
    }
}
