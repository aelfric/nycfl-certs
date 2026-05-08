package org.nycfl.certificates.lastround;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class NoS3OrKeycloakProfile implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "%test.quarkus.aws.devservices.localstack.enabled", "false",
            "%test.quarkus.aws.devservices.keycloak.enabled", "false"
        );
    }
}
