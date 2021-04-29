/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkiverse.logging.cloudwatch.it;

import static org.testcontainers.containers.localstack.LocalStackContainer.Service.CLOUDWATCH;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;

import com.amazonaws.services.s3.AmazonS3ClientBuilder;

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

public class LoggingCloudwatchResource implements QuarkusTestResourceLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingCloudwatchResource.class);

    private final LocalStackContainer localstack;

    public LoggingCloudwatchResource() {
        DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");
        this.localstack = new LocalStackContainer(localstackImage).withServices(S3, CLOUDWATCH);
    }

    @Override
    public Map<String, String> start() {
        LOGGER.info("Starting localstack CloudWatch docker container");
        localstack.start();
        LOGGER.info("CloudWatch docker container started");
        LoggingCloudwatchHandlerResourceTest.S3 = AmazonS3ClientBuilder
                .standard()
                .withEndpointConfiguration(localstack.getEndpointConfiguration(S3))
                .withCredentials(localstack.getDefaultCredentialsProvider())
                .build();

        return new HashMap<>();
    }

    @Override
    public void stop() {
        localstack.stop();
    }
}
