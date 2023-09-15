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
package io.quarkiverse.logging.cloudwatch.format;

import static co.elastic.logging.EcsJsonSerializer.toNullSafeString;

import java.util.Optional;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

import co.elastic.logging.EcsJsonSerializer;
import co.elastic.logging.JsonUtils;

public class ElasticCommonSchemaLogFormatter extends ExtFormatter {

    private Optional<String> serviceEnvironment;

    public ElasticCommonSchemaLogFormatter(Optional<String> serviceEnvironment) {
        this.serviceEnvironment = serviceEnvironment;
    }

    @Override
    public String format(ExtLogRecord record) {
        StringBuilder builder = new StringBuilder();

        EcsJsonSerializer.serializeObjectStart(builder, record.getMillis());
        EcsJsonSerializer.serializeLogLevel(builder, record.getLevel().getName());
        EcsJsonSerializer.serializeFormattedMessage(builder, record.getMessage());
        if (serviceEnvironment != null && serviceEnvironment.isPresent()) {
            serializeField(builder, serviceEnvironment.get());
        }
        EcsJsonSerializer.serializeThreadName(builder, record.getThreadName());
        EcsJsonSerializer.serializeLoggerName(builder, record.getLoggerName());
        EcsJsonSerializer.serializeMDC(builder, record.getMdcCopy());

        boolean includeOrigin = false;
        if (includeOrigin && record.getSourceFileName() != null && record.getSourceMethodName() != null) {
            EcsJsonSerializer.serializeOrigin(builder, record.getSourceFileName(), record.getSourceMethodName(),
                    record.getSourceLineNumber());
        }

        boolean stackTraceAsArray = false;
        EcsJsonSerializer.serializeException(builder, record.getThrown(), stackTraceAsArray);
        EcsJsonSerializer.serializeObjectEnd(builder);

        return builder.toString();
    }

    private void serializeField(StringBuilder builder, String value) {
        builder.append('"');
        JsonUtils.quoteAsString("service.environment", builder);
        builder.append("\":\"");
        JsonUtils.quoteAsString(toNullSafeString(value), builder);
        builder.append("\",");
    }
}
