package org.nycfl.certificates.slides;

import io.quarkus.test.junit.QuarkusTestProfile;

import java.util.Map;

public class NoLocalstackOrKeycloak implements QuarkusTestProfile {
    @Override
    public Map<String, String> getConfigOverrides() {
        return Map.of(
            "quarkus.s3.devservices.enabled", "false",
            "quarkus.keycloak.devservices.enabled", "false"
        );
    }
}
