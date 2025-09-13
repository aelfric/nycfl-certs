package org.nycfl.certificates;

import com.github.database.rider.cdi.api.DBRider;
import com.github.database.rider.core.api.configuration.DBUnit;
import com.github.database.rider.core.api.dataset.DataSet;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.*;
import jakarta.ws.rs.core.MediaType;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nycfl.certificates.results.Result;
import org.nycfl.certificates.util.H2DataTypeFactory;
import org.nycfl.certificates.util.RestAssuredJsonbExtension;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.BAD_REQUEST;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.nycfl.certificates.TestUtils.givenARegularUser;
import static org.nycfl.certificates.TestUtils.givenASuperUser;


@QuarkusTest
@DBRider
@DBUnit(schema = "public", caseSensitiveTableNames = true, cacheConnection = false,
        dataTypeFactoryClass = H2DataTypeFactory.class)
@TestHTTPEndpoint(CertificatesResource.class)
@QuarkusTestResource(OidcWiremockTestResource.class)
@ExtendWith(RestAssuredJsonbExtension.class)
class CertificatesResourceTest {
    @Inject
    EntityManager entityManager;

    @Inject
    UserTransaction transaction;

    @AfterEach
    void cleanUp() throws SystemException, NotSupportedException,
        HeuristicRollbackException, HeuristicMixedException, RollbackException {
        transaction.begin();
        List<Tournament> tournaments = entityManager
            .createQuery("SELECT t FROM Tournament t ", Tournament.class)
            .getResultList();
        tournaments.forEach(t -> entityManager.remove(t));
        transaction.commit();
    }

    @Test
    void createTournament() {

        Long numTourneysBefore = entityManager
            .createQuery("SELECT COUNT(distinct t.id) FROM Tournament t", Long.class)
            .getSingleResult();

        givenASuperUser()
            .body("""
                  {
                                "name": "NYCFL First Regis",
                                "host": "Regis High School",
                                "date": "2020-09-26"
                              }""")
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/tournaments")
            .then()
            .statusCode(200);

        Long numTourneysAfter = entityManager
            .createQuery("SELECT COUNT(distinct t.id) FROM Tournament t", Long.class)
            .getSingleResult();

        assertThat(numTourneysAfter).isEqualTo(numTourneysBefore + 1);
    }

    @Test
    @DataSet(cleanBefore = true, value = "two-tournaments.yml")
    void getAllTournaments() {
        assertThat(givenASuperUser()
            .contentType(MediaType.APPLICATION_JSON)
            .get("/tournaments")
            .as(Tournament[].class))
            .hasSize(2);
    }

    @Test
    @DataSet(cleanBefore = true, value = "one-tournament.yml")
    void updateTournament() {

        givenASuperUser()
            .body("""
                  {
                    "name": "Byram Hills Invitational",
                    "host": "Byram Hills High School",
                    "date": "2020-10-10",
                    "logoUrl": "https://s3.amazonaws.com/tabroom-files/tourns/16385/ByramBobcat.JPG",
                    "certificateHeadline": "Byram Hills Invitational Tournament",
                    "signature": "Someone Else"
                  }""")
            .contentType(MediaType.APPLICATION_JSON)
            .pathParam("id", 1)
            .when()
            .post("/tournaments/{id}")
            .then()
            .statusCode(200);

        Tournament tournamentAfterTest = entityManager.find(Tournament.class,
            1);

        assertThat(tournamentAfterTest.getLogoUrl()).isEqualTo(
            "https://s3.amazonaws.com/tabroom-files/tourns/16385/ByramBobcat.JPG");

    }

    @Test
    @DataSet(cleanBefore = true, value = "one-tournament.yml")
    void getTournament() {
        assertThat(givenASuperUser()
            .contentType(MediaType.APPLICATION_JSON)
            .pathParam("id", 1)
            .when()
            .get("/tournaments/{id}")
            .as(Tournament.class)
            .getName()
        ).isEqualTo("NYCFL First Regis");
    }

    @Test
    @DataSet(
        cleanBefore = true,
        value = "one-custom-tournament.yml",
        executeStatementsBefore = {
            "alter sequence Tournament_SEQ restart with 51"
        })
    @DisplayName("Clone a tournament that has been customized")
    void cloneTournament() {
        assertThat(givenASuperUser()
            .contentType(MediaType.APPLICATION_JSON)
            .queryParam("sourceId", 1)
            .when()
            .post("/tournaments")
            .as(Tournament.class))
            .hasFieldOrPropertyWithValue("name", "Copy of Byram Hills Invitational")
            .hasFieldOrPropertyWithValue("signature", "Someone Else");
    }

    @Test
    @DataSet(
        cleanBefore = true,
        value = "one-tournament.yml",
        executeStatementsBefore = {
            "alter sequence Tournament_SEQ restart with 51"
        })
    @DisplayName("Clone a tournament that has not been customized")
    void cloneTournament2() {
        assertThat(givenASuperUser()
            .contentType(MediaType.APPLICATION_JSON)
            .queryParam("sourceId", 1)
            .when()
            .post("/tournaments")
            .as(Tournament.class))
            .hasFieldOrPropertyWithValue("name", "Copy of NYCFL First Regis");
    }

    @Test
    @DataSet(cleanBefore = true, value = "one-tournament.yml")
    @DisplayName("Cannot clone a tournament with an invalid ID")
    void badCloneTournament() {
        givenASuperUser()
            .contentType(MediaType.APPLICATION_JSON)
            .queryParam("sourceId", 999)
            .when()
            .post("/tournaments")
            .then()
            .statusCode(NOT_FOUND);
    }

    @Test
    @DataSet(cleanBefore = true, value = "one-tournament.yml")
    @DisplayName("Cannot call create tournament with no sourceID or payload")
    void badCloneTournament2() {
        givenASuperUser()
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/tournaments")
            .then()
            .statusCode(BAD_REQUEST);
    }

    @Test
    @DataSet(cleanBefore = true, value = "one-tournament.yml")
    void createEvents() {

        givenASuperUser()
            .body(
                """
                {"tournamentId":"%d","events":"Junior Varsity Oral Interpretation\\nDuo Interpretation"}"""
                    .formatted(1))
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/events")
            .then()
            .statusCode(200);

        Long numEvents = entityManager
            .createQuery("SELECT COUNT(distinct e.id) FROM Event e WHERE e.tournament.id=?1",
                Long.class)
            .setParameter(1, 1)
            .getSingleResult();

        assertThat(numEvents).isEqualTo(2L);

    }

    @Test
    @DataSet(
        cleanBefore = true,
        value = {
            "one-tournament.yml",
            "events.yml"
        }
    )
    void abbreviateEvent() {

        assertThat(givenASuperUser()
            .queryParam("abbreviation", "JV OI")
            .pathParam("id", 1)
            .pathParam("evtId", 24)
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/tournaments/{id}/events/{evtId}/abbreviate")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString()).contains("JV OI");

        String abbreviation = entityManager
            .createQuery("SELECT e.abbreviation FROM Event e WHERE e.id=?1", String.class)
            .setParameter(1, 24)
            .getSingleResult();

        assertThat(abbreviation).isEqualTo("JV OI");

    }

    @Test
    @DataSet(cleanBefore = true, value = "one-tournament.yml")
    void requiresSuperuser() {
        givenARegularUser()
            .body("""
                  {
                  "tournamentId":"1",
                  "events":"Junior Varsity Oral Interpretation\\nDuo Interpretation"
                  }""")
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/events")
            .then()
            .statusCode(403);
    }

    @Test
    @DataSet(cleanBefore = true, value = "one-tournament.yml")
    void requiresAuth() {

        given()
            .body("""
                  {
                  "tournamentId":"1",
                  "events":"Junior Varsity Oral Interpretation\\nDuo Interpretation"
                  }""")
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/events")
            .then()
            .statusCode(401);
    }

    @Test
    @DataSet(
        cleanBefore = true,
        value = {
            "one-tournament.yml",
            "events.yml"
        }
    )
    void addSpeechResults() {
        givenASuperUser()
            .pathParam("eventId", 24)
            .pathParam("tournamentId", 1)
            .multiPart(new File("src/test/resources/JV-OI.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

        Long numResults = entityManager
            .createQuery(
                "SELECT COUNT(distinct r.id) FROM Result r WHERE r.event.id = ?1 and r.event.tournament.id=?2",
                Long.class)
            .setParameter(1, 24)
            .setParameter(2, 1)
            .getSingleResult();

        assertThat(numResults).isEqualTo(12L);

        givenASuperUser()
            .pathParam("eventId", 23)
            .pathParam("tournamentId", 1)
            .multiPart(new File("src/test/resources/duo.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

        numResults = entityManager
            .createQuery(
                "SELECT COUNT(distinct r.id) FROM Result r WHERE r.event.id = ?1 and r.event.tournament.id=?2",
                Long.class)
            .setParameter(1, 23)
            .setParameter(2, 1)
            .getSingleResult();

        assertThat(numResults).isEqualTo(2L);
    }

    @Test
    @DataSet(
        cleanBefore = true,
        value = {
            "one-tournament.yml",
            "events.yml"
        }
    )
    void addLDResults() {
        givenASuperUser()
            .queryParam("type", EliminationRound.QUARTER_FINALIST.name())
            .pathParam("eventId", 25)
            .pathParam("tournamentId", 1)
            .multiPart(new File("src/test/resources/ld-quarters.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

        givenASuperUser()
            .queryParam("type", EliminationRound.SEMIFINALIST.name())
            .pathParam("eventId", 25)
            .pathParam("tournamentId", 1)
            .multiPart(new File("src/test/resources/ld-semis.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

        givenASuperUser()
            .queryParam("type", EliminationRound.FINALIST.name())
            .pathParam("eventId", 25)
            .pathParam("tournamentId", 1)
            .multiPart(new File("src/test/resources/ld-finals.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

        Result quarterFinalist = entityManager
            .createQuery("SELECT r FROM Result r  where r.code=?1",
                Result.class)
            .setParameter(1, "Perry JSP")
            .getSingleResult();

        Result semiFinalist = entityManager
            .createQuery("SELECT r FROM Result r  where r.code=?1",
                Result.class)
            .setParameter(1, "Lexton PR")
            .getSingleResult();

        Result finalist = entityManager
            .createQuery("SELECT r FROM Result r  where r.code=?1",
                Result.class)
            .setParameter(1, "Harris MB")
            .getSingleResult();

        assertAll(
            () -> assertThat(quarterFinalist.getEliminationRound()).isEqualTo(EliminationRound.QUARTER_FINALIST),
            () -> assertThat(semiFinalist.getEliminationRound()).isEqualTo(EliminationRound.SEMIFINALIST),
            () -> assertThat(finalist.getEliminationRound()).isEqualTo(EliminationRound.FINALIST)
        );
    }

    @Test
    @DataSet(
        cleanBefore = true,
        value = {
            "one-tournament.yml",
            "events.yml"
        }
    )
    void addPFResults() {

        givenASuperUser()
            .queryParam("type", EliminationRound.QUARTER_FINALIST.name())
            .pathParam("eventId", 26)
            .pathParam("tournamentId", 1)
            .multiPart(new File("src/test/resources/pf-quarters.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

        givenASuperUser()
            .queryParam("type", EliminationRound.SEMIFINALIST.name())
            .pathParam("eventId", 26)
            .pathParam("tournamentId", 1)
            .multiPart(new File("src/test/resources/pf-semis.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

        givenASuperUser()
            .queryParam("type", EliminationRound.FINALIST.name())
            .pathParam("eventId", 26)
            .pathParam("tournamentId", 1)
            .multiPart(new File("src/test/resources/pf-finals.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

        Result quarterFinalist = entityManager
            .createQuery("SELECT r FROM Result r  where r.code=?1",
                Result.class)
            .setParameter(1, "Regis OZ")
            .getSingleResult();

        Result semiFinalist = entityManager
            .createQuery("SELECT r FROM Result r  where r.code=?1",
                Result.class)
            .setParameter(1, "Newton South GK")
            .getSingleResult();

        Result finalist = entityManager
            .createQuery("SELECT r FROM Result r  where r.code=?1",
                Result.class)
            .setParameter(1, "Regis GS")
            .getSingleResult();

        assertAll(
            () -> assertThat(quarterFinalist.getEliminationRound()).isEqualTo(EliminationRound.QUARTER_FINALIST),
            () -> assertThat(semiFinalist.getEliminationRound()).isEqualTo(EliminationRound.SEMIFINALIST),
            () -> assertThat(finalist.getEliminationRound()).isEqualTo(EliminationRound.FINALIST)
        );
    }

    @Test
    @DataSet(
        cleanBefore = true,
        value = {
            "one-tournament.yml",
            "events.yml"
        }
    )
    void addDebateSpeaks() {
        givenASuperUser()
            .pathParam("eventId", 27)
            .pathParam("tournamentId", 1)
            .multiPart(new File("src/test/resources/debate-speaks.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

        Result topSpeaks = entityManager
            .createQuery("SELECT r FROM Result r  where r.code=?1",
                Result.class)
            .setParameter(1, "Brookline FE")
            .getSingleResult();

        assertThat(topSpeaks.getPlace()).isOne();
    }

    @Test
    @DataSet(
        cleanBefore = true,
        value = "one-tournament.yml"
    )
    void addSweeps() {
        givenASuperUser()
            .pathParam("id", 1L)
            .multiPart(new File("src/test/resources/schools.csv"))
            .post("/tournaments/{id}/schools")
            .body();
        givenASuperUser()
            .pathParam("id", 1L)
            .multiPart(new File("src/test/resources/sweeps.csv"))
            .post("/tournaments/{id}/sweeps")
            .body();

        Integer regisSweeps = entityManager
            .createQuery(
                "select s.sweepsPoints FROM School s WHERE s.name = ?1 and s.tournament.id = ?2",
                Integer.class)
            .setParameter(1, "Regis")
            .setParameter(2, 1)
            .getSingleResult();
        assertThat(regisSweeps).isEqualTo(89);
    }

    @Test
    @DataSet(
        cleanBefore = true,
        value = "one-tournament.yml"
    )
    void deleteSchoolWithoutResults() {
        givenASuperUser()
            .pathParam("id", 1L)
            .multiPart(new File("src/test/resources/schools.csv"))
            .post("/tournaments/{id}/schools")
            .body();

        Long regisId = entityManager
            .createQuery("select s.id FROM School s WHERE s.name = ?1 and s" +
                    ".tournament.id = ?2",
                Long.class)
            .setParameter(1, "Regis")
            .setParameter(2, 1L)
            .getSingleResult();

        givenASuperUser()
            .pathParam("id", 1L)
            .pathParam("sid", regisId)
            .when()
            .delete("/tournaments/{id}/schools/{sid}")
            .then().statusCode(200);

        TypedQuery<Long> query = entityManager
            .createQuery("select s.id FROM School s WHERE s.name = ?1 and s" +
                    ".tournament.id = ?2",
                Long.class)
            .setParameter(1, "Regis")
            .setParameter(2, 1L);
        assertThatExceptionOfType(NoResultException.class).isThrownBy(query::getSingleResult);
    }

    @Test
    @DataSet(cleanBefore = true, value = {
        "one-tournament.yml",
        "events.yml",
        "results.yml"
    })
    void cannotDeleteSchoolWithResults() {
        givenASuperUser()
            .pathParam("id", 1L)
            .pathParam("sid", 119L)
            .when()
            .delete("/tournaments/{id}/schools/{sid}")
            .then().statusCode(HttpStatus.SC_BAD_REQUEST);
    }

    @Test
    @DataSet(cleanBefore = true, value = {
        "one-tournament.yml",
    })
    void getOneTournamentSweeps() {
        givenASuperUser()
            .pathParam("id", 1L)
            .multiPart(new File("src/test/resources/schools.csv"))
            .post("/tournaments/{id}/schools")
            .body();
        givenASuperUser()
            .pathParam("id", 1L)
            .multiPart(new File("src/test/resources/sweeps.csv"))
            .post("/tournaments/{id}/sweeps")
            .body();

        SweepsResult[] sweepsResults = givenARegularUser()
            .pathParam("id", 1L)
            .get("/tournaments/{id}/sweeps")
            .body()
            .as(SweepsResult[].class);

        assertThat(sweepsResults).hasSize(13);
    }

    @Test
    @DataSet(cleanBefore = true, value = {
        "two-tournaments.yml",
    })
    void getTwoTournamentSweeps() {
        givenASuperUser()
            .pathParam("id", 1L)
            .multiPart(new File("src/test/resources/schools.csv"))
            .post("/tournaments/{id}/schools")
            .body();
        givenASuperUser()
            .pathParam("id", 1L)
            .multiPart(new File("src/test/resources/sweeps.csv"))
            .post("/tournaments/{id}/sweeps")
            .body();
        givenASuperUser()
            .pathParam("id", 51L)
            .multiPart(new File("src/test/resources/schools.csv"))
            .post("/tournaments/{id}/schools")
            .body();
        givenASuperUser()
            .pathParam("id", 51L)
            .multiPart(new File("src/test/resources/sweeps2.csv"))
            .post("/tournaments/{id}/sweeps")
            .body();

        AggregateSweeps sweepsResults = givenARegularUser()
            .get("/tournaments/sweeps")
            .as(AggregateSweeps.class);

        assertThat(sweepsResults.totals)
            .containsEntry("Regis", 82 + 89)
            .containsEntry("Convent of the Sacred Heart, NYC", 97 + 79)
            .containsEntry("Democracy Prep Harlem Prep", 39 + 13);
    }

    @Test
    @DataSet(
        cleanBefore = true,
        value = {
            "one-tournament.yml",
            "events.yml",
            "results.yml"
        }
    )
    void listSchools() {
        School[] schools = givenARegularUser()
            .pathParam("id", 1L)
            .get("/tournaments/{id}/schools")
            .as(School[].class);

        assertThat(schools)
            .extracting(School::getName)
            .contains("Convent of the Sacred Heart",
                "Bronx Science",
                "Regis",
                "Democracy Prep Harlem Prep");
    }

    @Test
    @DataSet(
        cleanBefore = true,
        value = "one-tournament.yml"
    )
    void addSchools() {
        School[] schools = givenASuperUser()
            .pathParam("id", 1)
            .multiPart(new File("src/test/resources/schools.csv"))
            .post("/tournaments/{id}/schools")
            .as(School[].class);

        assertThat(schools)
            .extracting(School::getName)
            .contains("Byram Hills",
                "Convent of the Sacred Heart, NYC",
                "Democracy Prep Endurance",
                "Democracy Prep Harlem",
                "Democracy Prep Harlem Prep",
                "Iona Prep",
                "Monsignor Farrell",
                "Pelham Memorial",
                "Regis",
                "Scarsdale",
                "Stuyvesant",
                "Bronx Science",
                "Xavier");
    }

    @Test
    @DataSet(cleanBefore = true, value = {
        "one-tournament.yml",
        "events.yml",
        "results.yml"
    })
    void clearResults() {
        givenASuperUser()
            .pathParam("eventId", 24L)
            .pathParam("tournamentId", 1L)
            .delete("/tournaments/{tournamentId}/events/{eventId}/results");

        Event jvOIAfter = entityManager.find(Event.class, 24L);
        assertThat(jvOIAfter.getResults()).isEmpty();
    }

    @Test
    @DataSet(cleanBefore = true, value = {
        "one-tournament.yml",
        "events.yml",
        "results.yml"
    })
    void renameResult() {
        givenASuperUser()
            .pathParam("evtId", 24L)
            .pathParam("id", 1L)
            .pathParam("resultId", 138L)
            .queryParam("name", "Johnny Newname")
            .contentType(MediaType.APPLICATION_JSON)
            .post("/tournaments/{id}/events/{evtId}/results/{resultId}/rename")
            .then()
            .statusCode(200);

        Result result = entityManager.find(Result.class, 138L);

        assertThat(result.getName()).isEqualTo("Johnny Newname");
    }

    @Test
    @DataSet(cleanBefore = true, value = {
        "one-tournament.yml",
        "events.yml",
        "results.yml"
    })
    void renameResultCanFail() {

        givenASuperUser()
            .pathParam("evtId", 0)
            .pathParam("id", 1)
            .pathParam("resultId", 138L)
            .queryParam("name", "Johnny Newname")
            .contentType(MediaType.APPLICATION_JSON)
            .post("/tournaments/{id}/events/{evtId}/results/{resultId}/rename")
            .then()
            .statusCode(404);

        Result result = entityManager.find(Result.class, 138L);

        assertThat(result.getName()).isEqualTo("Carina Dillard");
    }

    @Test
    @DataSet(cleanBefore = true, value = {
        "one-tournament.yml",
        "events.yml",
        "results.yml"
    })
    void setPlacementCutoff() {
        givenASuperUser()
            .pathParam("id", 1L)
            .pathParam("evtId", 24L)
            .body("""
                  {"cutoff":"3"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/tournaments/{id}/events/{evtId}/placement")
            .then()
            .statusCode(200);

        Event jvOIAfter = entityManager.find(Event.class, 24L);
        assertThat(jvOIAfter.getPlacementCutoff()).isEqualTo(3);
    }

    @Test
    @DataSet(cleanBefore = true, value = {
        "one-tournament.yml",
        "events.yml",
        "results.yml"
    })
    void setCertificateCutoff() {
        givenASuperUser()
            .pathParam("eventId", 24L)
            .pathParam("tournamentId", 1L)
            .multiPart(new File("src/test/resources/JV-OI.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

        givenASuperUser()
            .pathParam("id", 1L)
            .pathParam("evtId", 24L)
            .body("""
                  {"cutoff":"3"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/tournaments/{id}/events/{evtId}/cutoff")
            .then()
            .statusCode(200);

        Event jvOIAfter = entityManager.find(Event.class, 24L);
        assertThat(jvOIAfter.getCertificateCutoff()).isEqualTo(3);

    }

    @Test
    @DataSet(cleanBefore = true, value = {
        "one-tournament.yml",
        "events.yml",
        "results.yml"
    })
    void setCertificateType() {
        givenASuperUser()
            .pathParam("id", 1L)
            .pathParam("evtId", 24L)
            .queryParam("type", CertificateType.QUALIFIER)
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/tournaments/{id}/events/{evtId}/cert_type")
            .then()
            .statusCode(200);

        Event jvOIAfter = entityManager.find(Event.class, 24L);
        assertThat(jvOIAfter.getCertificateType()).isEqualTo(CertificateType.QUALIFIER);

    }

    @Test
    @DataSet(cleanBefore = true, value = {
        "one-tournament.yml",
        "events.yml",
        "results.yml"
    })
    void setStateQualCutoff() {
        givenASuperUser()
            .pathParam("id", 1L)
            .pathParam("evtId", 24L)
            .body("{\"cutoff\":\"3\"}")
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/tournaments/{id}/events/{evtId}/quals")
            .then()
            .statusCode(200);

        Event jvOIAfter = entityManager.find(Event.class, 24L);
        assertThat(jvOIAfter.getHalfQuals()).isEqualTo(3);
    }

    @Test
    @DataSet(cleanBefore = true, value = {
        "one-tournament.yml",
        "events.yml",
    })
    void changeEventType() {
        givenASuperUser()
            .pathParam("eventId", 24L)
            .pathParam("tournamentId", 1L)
            .queryParam("type", EventType.DEBATE_LD.name())
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/type")
            .then()
            .statusCode(200);

        Event ldAfter = entityManager.find(Event.class, 25L);
        assertThat(ldAfter.getEventType()).isEqualTo(EventType.DEBATE_LD);

    }

    @Test
    @DataSet(cleanBefore = true, value = {
        "one-tournament.yml",
        "events.yml"
    })
    void deleteEvent() {
        givenASuperUser()
            .pathParam("eventId", 24L)
            .pathParam("tournamentId", 1)
            .queryParam("type", EventType.DEBATE_LD.name())
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .delete("/tournaments/{tournamentId}/events/{eventId}")
            .then()
            .statusCode(200);

        assertThat(entityManager.find(Event.class, 24L)).isNull();

    }

    @Test
    @DataSet(
        cleanBefore = true,
        value = {
            "one-tournament.yml",
            "events.yml"
        }
    )
    void createSpeakerAwards() {
        givenASuperUser()
            .pathParam("eventId", 25)
            .pathParam("tournamentId", 1)
            .queryParam("type", EventType.DEBATE_LD.name())
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/type")
            .then()
            .statusCode(200);

        List<Event> ldAfter = entityManager.createQuery("SELECT e FROM Event e WHERE e" +
            ".eventType='DEBATE_SPEAKS'", Event.class).getResultList();
        assertThat(ldAfter).hasSize(2);

    }

    @Test
    @DataSet(cleanBefore = true, value = {
        "one-tournament.yml",
        "events.yml",
        "results.yml"
    })
    void setMedalCutoff() {
        givenASuperUser()
            .pathParam("id", 1L)
            .pathParam("evtId", 24L)
            .body("""
                  {"cutoff":"3"}""")
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/tournaments/{id}/events/{evtId}/medal")
            .then()
            .statusCode(200);

        Event jvOIAfter = entityManager.find(Event.class, 24L);
        assertThat(jvOIAfter.getMedalCutoff()).isEqualTo(3);
    }

    @Test
    @DataSet(
        cleanBefore = true,
        value = {
            "full-tournament.yml"
        }
    )
    void generateCertificates() {
        String certificateHtml = givenARegularUser()
            .pathParam("id", 1)
            .get("/tournaments/{id}/certificates")
            .asString();

        assertThat(certificateHtml)
            .contains("Finalist")
            .contains("Fifth Place")
            .contains("First Place")
            .contains("Leticia Irving")
            .doesNotContain("River Weaver")
            .containsPattern(Pattern.compile("(Junior Varsity Oral Interpretation.*?){8}",
                Pattern.MULTILINE | Pattern.DOTALL))
            .containsPattern(Pattern.compile("(Duo Interpretation.*?){2}",
                Pattern.MULTILINE | Pattern.DOTALL));
    }

    @Test
    @DataSet(
        cleanBefore = true,
        value = {
            "full-tournament.yml"
        }
    )
    void getMedalCount() {
        MedalCount[] medalCounts = givenARegularUser()
            .pathParam("id", 1L)
            .get("/tournaments/{id}/medals")
            .as(MedalCount[].class);

        assertThat(medalCounts).contains(
            new MedalCount("Regis", 5, 4),
            new MedalCount("Bronx Science", 1, 2),
            new MedalCount("Convent of the Sacred Heart", 1, 3)
        );
    }
}