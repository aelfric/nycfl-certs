package org.nycfl.certificates.util;

import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.junit.jupiter.api.extension.*;

public class RestAssuredJsonbExtension implements BeforeAllCallback, AfterAllCallback,
    ParameterResolver {
    static Jsonb jsonb;

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        jsonb.close();
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (jsonb == null) {
            jsonb = JsonbBuilder.create();
            ObjectMapper mapper = new ObjectMapper() {
                public Object deserialize(ObjectMapperDeserializationContext context) {
                    return jsonb.fromJson(context.getDataToDeserialize().asString(),
                        context.getType());
                }

                public Object serialize(ObjectMapperSerializationContext context) {
                    return jsonb.toJson(context.getObjectToSerialize());
                }
            };
            RestAssured.config = RestAssured.config().objectMapperConfig(
                ObjectMapperConfig.objectMapperConfig().defaultObjectMapper(mapper)
            );
        }
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext,
                                     ExtensionContext extensionContext) throws ParameterResolutionException {
        return parameterContext.getParameter().getType()
            .equals(Jsonb.class);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext,
                                   ExtensionContext extensionContext) throws ParameterResolutionException {
        return jsonb;
    }
}
