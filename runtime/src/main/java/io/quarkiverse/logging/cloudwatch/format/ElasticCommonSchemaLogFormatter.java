package io.quarkiverse.logging.cloudwatch.format;

import static co.elastic.logging.EcsJsonSerializer.toNullSafeString;

import org.jboss.logmanager.ExtFormatter;
import org.jboss.logmanager.ExtLogRecord;

import co.elastic.logging.EcsJsonSerializer;
import co.elastic.logging.JsonUtils;
import io.quarkus.runtime.configuration.ProfileManager;

public class ElasticCommonSchemaLogFormatter extends ExtFormatter {

    private final String environment = ProfileManager.getActiveProfile();

    @Override
    public String format(ExtLogRecord record) {
        StringBuilder builder = new StringBuilder();

        EcsJsonSerializer.serializeObjectStart(builder, record.getMillis());
        EcsJsonSerializer.serializeLogLevel(builder, record.getLevel().getName());
        EcsJsonSerializer.serializeFormattedMessage(builder, this.formatMessage(record));
        serializeField(builder, this.environment);
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
