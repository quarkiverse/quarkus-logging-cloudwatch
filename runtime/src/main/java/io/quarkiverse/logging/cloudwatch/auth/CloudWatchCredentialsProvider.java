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
package io.quarkiverse.logging.cloudwatch.auth;

import io.quarkiverse.logging.cloudwatch.LoggingCloudWatchConfig;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

public class CloudWatchCredentialsProvider implements AwsCredentialsProvider {

    private final LoggingCloudWatchConfig config;

    public CloudWatchCredentialsProvider(LoggingCloudWatchConfig config) {
        this.config = config;
    }

    @Override
    public AwsCredentials resolveCredentials() {
        return config.defaultCredentialsProviderEnabled ? DefaultCredentialsProvider.create().resolveCredentials()
                : new CloudWatchCredentials(config);
    }
}
