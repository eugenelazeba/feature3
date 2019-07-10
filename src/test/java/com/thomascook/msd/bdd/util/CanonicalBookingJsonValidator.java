package com.thomascook.msd.bdd.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.fge.jackson.JsonNodeReader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ListReportProvider;
import com.github.fge.jsonschema.core.report.LogLevel;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import org.apache.commons.lang3.Validate;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.IOException;

public class CanonicalBookingJsonValidator {

    private final JsonSchema jsonSchema;

    public CanonicalBookingJsonValidator(ResourceLoader resourceLoader, String jsonSchemaPath) throws IOException, ProcessingException {
        Validate.notNull(resourceLoader);

        Resource schemaFile = resourceLoader.getResource(jsonSchemaPath);
        Validate.isTrue(schemaFile.exists(), "JSON schema not found at [" + jsonSchemaPath + "]");

        this.jsonSchema = JsonSchemaFactory.newBuilder()
                .setReportProvider(new ListReportProvider(LogLevel.ERROR, LogLevel.FATAL))
                .freeze()
                .getJsonSchema(new JsonNodeReader().fromInputStream(schemaFile.getInputStream()));
    }

    public ProcessingReport check(JsonNode json) {
        try {
            return jsonSchema.validate(json, true);
        } catch (ProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
