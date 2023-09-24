package org.nycfl.certificates;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@QuarkusTest
@TestHTTPEndpoint(EnumResource.class)
class EnumResourceTest {
    @Test
    void getEventTypes() {
        given()
            .get("event_types")
            .then()
            .body(
                containsString("Debate"),
                containsString("DEBATE")
            );
    }

    @Test
    void getCertificateTypes() {
        given()
            .get("certificate_types")
            .then()
            .body(
                containsString("Top Speaker"),
                containsString("DEBATE_SPEAKER")
            );
    }

    @Test
    void getElimTypes() {
        given()
            .get("elim_types")
            .then()
            .body(
                containsString("FINALIST"),
                containsString("Finalist")
            );
    }

    @Test
    void getCertTypes() {
        given()
            .get("certificate_types")
            .then()
            .body(
                "[0].label", equalTo("Placement"))
            .body(
                "[0].value", equalTo("PLACEMENT")
            );
    }
}