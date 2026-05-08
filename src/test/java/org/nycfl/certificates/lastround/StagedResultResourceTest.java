package org.nycfl.certificates.lastround;

import com.github.database.rider.cdi.api.DBRider;
import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.core.api.dataset.ExpectedDataSet;
import com.github.database.rider.core.api.exporter.ExportDataSet;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nycfl.certificates.util.H2DataTypeFactory;
import org.nycfl.certificates.util.RestAssuredJsonbExtension;

import java.io.File;

import static org.nycfl.certificates.TestUtils.givenASuperUser;


@QuarkusTest
@DBRider
@DBUnit(caseSensitiveTableNames = true, escapePattern = "\"?\"", schema = "PUBLIC", dataTypeFactoryClass = H2DataTypeFactory.class)
@TestHTTPEndpoint(LastRoundResource.class)
@QuarkusTestResource(OidcWiremockTestResource.class)
@ExtendWith(RestAssuredJsonbExtension.class)
@TestProfile(NoS3OrKeycloakProfile.class)
class StagedResultResourceTest {

    @DataSet(cleanBefore = true, value = "one-tournament.yml")
    @ExpectedDataSet("last-results-upload.yml")
    @ExportDataSet(includeTables = "LASTROUNDIMPORT", dependentTables = true)
    @Test
    void uploadResults(){
        givenASuperUser()
            .pathParam("tournamentId", 1)
            .multiPart(new File("src/test/resources/last-round.csv"))
            .when()
            .post("/tournaments/{tournamentId}/bulk_results")
            .then()
            .statusCode(200);

    }

    @DataSet(cleanBefore = true, value = "last-round-imported.yml")
//    @ExpectedDataSet("last-results-upload.yml")
    @ExportDataSet(includeTables = "LASTROUNDIMPORT", dependentTables = true)
    @Test
    void getMappings(){
        givenASuperUser()
            .pathParam("tournamentId", 1)
            .pathParam("reportId", 1)
            .when()
            .get("/tournaments/{tournamentId}/bulk_results/{reportId}/mappings")
            .then()
            .statusCode(200)
            .log()
            .body(true);

    }

    @DataSet(cleanBefore = true, value = {"last-round-imported.yml","events.yml"})
//    @ExpectedDataSet("last-results-upload.yml")
    @ExportDataSet(includeTables = "LASTROUNDIMPORT", dependentTables = true)
    @Test
    void setMappings(){
        givenASuperUser()
            .contentType(ContentType.JSON)
            .pathParam("tournamentId", 1)
            .pathParam("reportId", 1)
            .body("""
                {
                    "OO": 23,
                    "OI": 24
                }
                """)
            .when()
            .post("/tournaments/{tournamentId}/bulk_results/{reportId}/mappings/events")
            .then()
            .statusCode(200)
            .log()
            .body(true);

    }
    @DataSet(cleanBefore = true, value = {"last-round-imported.yml","events.yml"})
//    @ExpectedDataSet("last-results-upload.yml")
    @ExportDataSet(includeTables = "LASTROUNDIMPORT", dependentTables = true)
    @Test
    void getRoundMapping(){
        givenASuperUser()
            .contentType(ContentType.JSON)
            .pathParam("tournamentId", 1)
            .pathParam("reportId", 1)
            .pathParam("mappingId", 1)
            .when()
            .get("/tournaments/{tournamentId}/bulk_results/{reportId}/mappings/{mappingId}")
            .then()
            .statusCode(200)
            .log()
            .body(true);

    }

}