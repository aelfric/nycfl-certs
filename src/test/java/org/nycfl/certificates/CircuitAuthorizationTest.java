package org.nycfl.certificates;

import com.github.database.rider.cdi.api.DBRider;
import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.dataset.DataSet;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.ws.rs.core.MediaType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.nycfl.certificates.util.H2DataTypeFactory;
import org.nycfl.certificates.util.RestAssuredJsonbExtension;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsStringIgnoringCase;

@QuarkusTest
@DBRider
@DBUnit(schema = "public", caseSensitiveTableNames = true, cacheConnection = false,
    dataTypeFactoryClass = H2DataTypeFactory.class, mergeDataSets = true)
@TestHTTPEndpoint(CertificatesResource.class)
@QuarkusTestResource(OidcWiremockTestResource.class)
@ExtendWith(RestAssuredJsonbExtension.class)
@DataSet(cleanBefore = true, value = {"one-tournament.yml", "events.yml", "results.yml"})
class CircuitAuthorizationTest {
    @Inject
    EntityManager entityManager;

    @MethodSource("protectedEndpoints")
    @ParameterizedTest
    @DisplayName("Superuser can manage any tournament")
    void superUserManage(EndpointSpec spec) {
        spec.apply(
                TestUtils.getToken(Collections.singleton("superuser"))
            )
            .statusCode(200);
    }

    @ParameterizedTest
    @MethodSource("protectedEndpoints")
    @DisplayName("Circuit manager can modify tournaments in their circuit")
    void circuitManagerManage(EndpointSpec spec) {
        spec.apply(TestUtils.getToken(Set.of("basicuser", "manage-circuit-nycfl")))
            .statusCode(200);
    }

    @ParameterizedTest
    @MethodSource("protectedEndpoints")
    @DisplayName("Circuit manager can manage multiple circuits")
    void multipleCircuits(EndpointSpec spec) {
        spec.apply(TestUtils.getToken(Set.of("basicuser", "manage-circuit-nycfl", "manage-circuit-odl")))
            .statusCode(200);
    }

    @ParameterizedTest
    @MethodSource("protectedEndpoints")
    @DisplayName("Circuit manager gets 403 for tournaments in other circuits")
    void differentCircuit(EndpointSpec spec) {
        spec.apply(TestUtils.getToken(Set.of("basicuser", "manage-circuit-odl")))
            .statusCode(403)
            .body(containsStringIgnoringCase("you are not permitted to manage circuit nycfl"));
    }

    @ParameterizedTest
    @MethodSource("protectedEndpoints")
    @DisplayName("Regular basicuser still gets 403 (existing behavior)")
    void basicUser(EndpointSpec spec) {
        spec.apply(TestUtils.getToken(Set.of("basicuser")))
            .statusCode(403);
    }

    @Test
    @DisplayName("Superuser can assign a tournament to a circuit")
    void superUserAssign() {
        EndpointSpec spec = new EndpointSpec(
            "PATCH",
            "/tournaments/{id}",
            Map.of("id",1),
            Map.of("circuit","odl"),
            null,
            null
        );
        spec.apply(TestUtils.getToken(Set.of("superuser")))
            .statusCode(200);
        Tournament after = entityManager.find(Tournament.class, 1L);
        assertThat(after.getCircuitId()).isEqualTo("odl");
    }

    @Test
    @DisplayName("Basic user cannot assign a tournament to a circuit")
    void basicUserAssign() {
        EndpointSpec spec = new EndpointSpec(
            "PATCH",
            "/tournaments/{id}",
            Map.of("id",1),
            Map.of("circuit","odl"),
            null,
            null
        );
        spec.apply(TestUtils.getToken(Set.of("basicuser")))
            .statusCode(403);
        Tournament after = entityManager.find(Tournament.class, 1L);
        assertThat(after.getCircuitId()).isEqualTo("nycfl");
    }

    private static Stream<EndpointSpec> protectedEndpoints() {
        return Stream.of(
            new EndpointSpec("POST",
                "/tournaments/{id}",
                Map.of("id", 1),
                Map.of(),
                """
                    {"name":"Updated", "date": "2020-09-26"}""",
                null),
            new EndpointSpec("POST",
                "/events",
                Map.of(),
                Map.of(),
                """
                    {"tournamentId":1,"events":"Test"}""",
                null),
            new EndpointSpec("POST",
                "/tournaments/{id}/events/{evtId}/type",
                Map.of("id", 1, "evtId", 24L),
                Map.of("type", "SPEECH"),
                null,
                null),
            new EndpointSpec("POST",
                "/tournaments/{id}/events/{evtId}/rename",
                Map.of("id", 1, "evtId", 24),
                Map.of(),
                null,
                null),
            new EndpointSpec("POST",
                "/tournaments/{id}/events/{evtId}/abbreviate",
                Map.of("id", 1, "evtId", 24),
                Map.of("abbreviation", "ABBR"),
                null,
                null),
            new EndpointSpec("POST",
                "/tournaments/{id}/events/{evtId}/cutoff",
                Map.of("id", 1, "evtId", 24),
                Map.of(), """
                {"cutoff":"3"}""",
                null),
            new EndpointSpec("POST",
                "/tournaments/{id}/events/{evtId}/medal",
                Map.of("id", 1, "evtId", 24),
                Map.of(),
                """
                    {"cutoff":"3"}""",
                null),
            new EndpointSpec("POST",
                "/tournaments/{id}/events/{evtId}/slide_size",
                Map.of("id", 1, "evtId", 24),
                Map.of(),
                """
                    {"cutoff":"3"}""", null),
            new EndpointSpec("POST",
                "/tournaments/{id}/events/{evtId}/quals",
                Map.of("id", 1, "evtId", 24),
                Map.of(),
                """
                    {"cutoff":"3"}""",
                null),
            new EndpointSpec("DELETE",
                "/tournaments/{id}/events/{eventId}",
                Map.of("id", 1, "eventId", 24),
                Map.of(),
                null,
                null),
            new EndpointSpec("DELETE",
                "/tournaments/{id}/events/{eventId}/results",
                Map.of("id", 1, "eventId", 24),
                Map.of(),
                null,
                null),
            new EndpointSpec("POST",
                "/tournaments/{id}/events/{evtId}/cert_type",
                Map.of("id", 1, "evtId", 24),
                Map.of("type", "PLACEMENT"),
                null,
                null),
            new EndpointSpec("POST",
                "/tournaments/{id}/events/{evtId}/rounds",
                Map.of("id", 1, "evtId", 24),
                Map.of("rounds", "4"),
                null,
                null),
            new EndpointSpec("POST",
                "/tournaments/{id}/events/{evtId}/placement",
                Map.of("id", 1, "evtId", 24),
                Map.of("type", "PLACEMENT"), """
                {"cutoff":"3"}""",
                null),
            new EndpointSpec("POST",
                "/tournaments/{id}/events/{evtId}/results/{resultId}/school",
                Map.of("id", 1, "evtId", 24, "resultId", 131),
                Map.of("schoolId", "117"),
                null,
                null),
            new EndpointSpec("POST",
                "/tournaments/{id}/schools",
                Map.of("id", 1 ),
                Map.of("schoolId", "117"),
                null,
                new File("src/test/resources/schools.csv")),
            new EndpointSpec("POST",
                "/tournaments/{tournamentId}/events/{eventId}/results",
                Map.of("tournamentId", 1, "eventId", 24),
                Map.of(),
                null,
                new File("src/test/resources/JV-OI.csv")),
            new EndpointSpec("POST",
                "/tournaments/{id}/sweeps",
                Map.of("id", 1),
                Map.of(),
                null,
                new File("src/test/resources/sweeps.csv"))
        );
    }

    record EndpointSpec(
        String method,
        String path,
        Map<String, Object> pathParams,
        Map<String, String> queryParams,
        String body,
        java.io.File multipartFile) {
        ValidatableResponse apply(String authToken) {
            RequestSpecification req = RestAssured.given()
                .auth()
                .oauth2(authToken)
                .contentType(MediaType.APPLICATION_JSON);
            if (body != null) {
                req.body(body);
            }
            if( multipartFile != null){
                req.multiPart(multipartFile);
                req.contentType(MediaType.MULTIPART_FORM_DATA);
            }

            req.pathParams(pathParams);
            req.queryParams(queryParams);

            return req
                .when()
                .request(method, path)
                .then();
        }
    }
}
