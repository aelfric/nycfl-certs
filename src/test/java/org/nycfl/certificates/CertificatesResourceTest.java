package org.nycfl.certificates;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.transaction.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;

@QuarkusTest
@TestHTTPEndpoint(CertificatesResource.class)
class CertificatesResourceTest {
  @Inject
  EntityManager entityManager;
  private final String tournamentJson = """
      {
        "name": "NYCFL First Regis",
        "host": "Regis High School",
        "date": "2020-09-26"
      }""";

  @Inject
  UserTransaction transaction;

  private final String events = "Junior Varsity Oral Interpretation\nDuo Interpretation";

  @Test
  void createTournament() {

    Long numTourneysBefore = entityManager
        .createQuery("SELECT COUNT(distinct t.id) FROM Tournament t", Long.class)
        .getSingleResult();

    given()
        .body(tournamentJson)
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .post("/tournaments")
        .then()
        .statusCode(200);

    Long numTourneysAfter = entityManager
        .createQuery("SELECT COUNT(distinct t.id) FROM Tournament t", Long.class)
        .getSingleResult();

    assertThat(numTourneysAfter, CoreMatchers.is(numTourneysBefore + 1));
  }

  @Test
  void testCreateEvents() throws HeuristicRollbackException, RollbackException, HeuristicMixedException, SystemException, NotSupportedException {
    transaction.begin();
    Jsonb jsonb = JsonbBuilder.create();
    Tournament tournament = jsonb.fromJson(tournamentJson, Tournament.class);
    entityManager.persist(tournament);
    transaction.commit();

    given()
        .body("{\"tournamentId\":\"%d\",\"events\":\"Junior Varsity Oral Interpretation\\nDuo Interpretation\"}"
            .formatted(tournament.getId()))
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .post("/events")
        .then()
        .statusCode(200);

    Long numEvents = entityManager
        .createQuery("SELECT COUNT(distinct e.id) FROM Event e WHERE e.tournament.id=?1", Long.class)
        .setParameter(1, tournament.getId())
        .getSingleResult();

    assertThat(numEvents, CoreMatchers.is(2L));

  }

  @Test
  void addResults() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Jsonb jsonb = JsonbBuilder.create();
    Tournament tournament = jsonb.fromJson(tournamentJson, Tournament.class);
    Event jvOI = new Event();
    jvOI.setName("Junior Varsity Oral Interpretation");
    jvOI.setTournament(tournament);
    Event duo = new Event();
    duo.setName("Duo Interpretation");
    duo.setTournament(tournament);
    tournament.events = Arrays.asList(
        jvOI,
        duo
    );
    entityManager.persist(tournament);
    transaction.commit();

    given()
        .queryParam("eventId", jvOI.getId())
        .queryParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .when()
        .post("/results")
        .then()
        .statusCode(200);

    Long numResults = entityManager
        .createQuery("SELECT COUNT(distinct r.id) FROM Result r WHERE r.event.id = ?1 and r.event.tournament.id=?2", Long.class)
        .setParameter(1, jvOI.getId())
        .setParameter(2, tournament.getId())
        .getSingleResult();

    assertThat(numResults, CoreMatchers.is(12L));

    given()
        .queryParam("eventId", duo.getId())
        .queryParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/duo.csv"))
        .when()
        .post("/results")
        .then()
        .statusCode(200);

    numResults = entityManager
        .createQuery("SELECT COUNT(distinct r.id) FROM Result r WHERE r.event.id = ?1 and r.event.tournament.id=?2", Long.class)
        .setParameter(1, duo.getId())
        .setParameter(2, tournament.getId())
        .getSingleResult();

    assertThat(numResults, CoreMatchers.is(2L));
  }

  @Test
  void testAddSweeps() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Jsonb jsonb = JsonbBuilder.create();
    Tournament tournament = jsonb.fromJson(tournamentJson, Tournament.class);
    entityManager.persist(tournament);
    transaction.commit();

    given()
        .pathParam("id", tournament.getId())
        .multiPart(new File("src/test/resources/schools.csv"))
        .post("/tournaments/{id}/schools")
        .body();
    given()
        .pathParam("id", tournament.getId())
        .multiPart(new File("src/test/resources/sweeps.csv"))
        .post("/tournaments/{id}/sweeps")
        .body();

    Integer regisSweeps = entityManager
        .createQuery("select s.sweepsPoints FROM School s WHERE s.name = ?1 and s.tournament.id = ?2",
            Integer.class)
        .setParameter(1, "Regis")
        .setParameter(2, tournament.getId())
        .getSingleResult();
    assertThat(regisSweeps, CoreMatchers.is(89));
  }

  @Test
  void listAllTournaments() {
    Long numTourneys = entityManager
        .createQuery("SELECT COUNT(t.id) FROM Tournament t", Long.class)
        .getSingleResult();

    List<Tournament> tournaments = given()
        .get("/tournaments")
        .body()
        .as(
            new ArrayList<Tournament>() {
            }.getClass().getGenericSuperclass());

    assertThat(tournaments, hasSize(Math.toIntExact(numTourneys)));
  }

  @Test
  void listSchools() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Jsonb jsonb = JsonbBuilder.create();
    Tournament tournament = jsonb.fromJson(tournamentJson, Tournament.class);
    Event jvOI = new Event();
    jvOI.setName("Junior Varsity Oral Interpretation");
    jvOI.setTournament(tournament);
    tournament.events = Collections.singletonList(
        jvOI
    );
    entityManager.persist(tournament);
    transaction.commit();

    given()
        .queryParam("eventId", jvOI.getId())
        .queryParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .when()
        .post("/results");

    List<School> schools = given()
        .pathParam("id", tournament.getId())
        .get("/tournaments/{id}/schools")
        .body().as(
            new ArrayList<School>() {
            }.getClass().getGenericSuperclass());
    assertThat(
        schools.stream().map(School::getName).collect(Collectors.toList()),
        CoreMatchers.hasItems(
            "Convent of the Sacred Heart",
            "Bronx Science",
            "Regis",
            "Democracy Prep Harlem Prep"
        )
    );
  }

  @Test
  void addSchools() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Jsonb jsonb = JsonbBuilder.create();
    Tournament tournament = jsonb.fromJson(tournamentJson, Tournament.class);
    entityManager.persist(tournament);
    transaction.commit();

    String schoolsJson = given()
        .pathParam("id", tournament.getId())
        .multiPart(new File("src/test/resources/schools.csv"))
        .post("/tournaments/{id}/schools")
        .body().print();

    List<School> schools = jsonb.fromJson(
        schoolsJson,
        new ArrayList<School>() {
        }.getClass().getGenericSuperclass());
    assertThat(
        schools.stream().map(School::getName).collect(Collectors.toList()),
        CoreMatchers.hasItems(
            "Byram Hills",
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
            "Xavier"
        )
    );
  }

  @Test
  void setPlacementCutoff() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Jsonb jsonb = JsonbBuilder.create();
    Tournament tournament = jsonb.fromJson(tournamentJson, Tournament.class);
    Event jvOI = new Event();
    jvOI.setName("Junior Varsity Oral Interpretation");
    jvOI.setTournament(tournament);
    tournament.events = Collections.singletonList(
        jvOI
    );
    entityManager.persist(tournament);
    transaction.commit();

    given()
        .queryParam("eventId", jvOI.getId())
        .queryParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .when()
        .post("/results")
        .then()
        .statusCode(200);

    given()
        .pathParam("id", tournament.getId())
        .pathParam("evtId", jvOI.getId())
        .body("{\"cutoff\":\"3\"}")
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .post("/tournaments/{id}/events/{evtId}/placement")
        .then()
        .statusCode(200);

    Event jvOIAfter = entityManager.find(Event.class, jvOI.getId());
    assertThat(jvOIAfter.getPlacementCutoff(), CoreMatchers.is(3));
  }

  @Test
  void setCertificateCutoff() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Jsonb jsonb = JsonbBuilder.create();
    Tournament tournament = jsonb.fromJson(tournamentJson, Tournament.class);
    Event jvOI = new Event();
    jvOI.setName("Junior Varsity Oral Interpretation");
    jvOI.setTournament(tournament);
    tournament.events = Collections.singletonList(
        jvOI
    );
    entityManager.persist(tournament);
    transaction.commit();

    given()
        .queryParam("eventId", jvOI.getId())
        .queryParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .when()
        .post("/results")
        .then()
        .statusCode(200);

    given()
        .pathParam("id", tournament.getId())
        .pathParam("evtId", jvOI.getId())
        .body("{\"cutoff\":\"3\"}")
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .post("/tournaments/{id}/events/{evtId}/cutoff")
        .then()
        .statusCode(200);

    Event jvOIAfter = entityManager.find(Event.class, jvOI.getId());
    assertThat(jvOIAfter.getCertificateCutoff(), CoreMatchers.is(3));

  }

  @Test
  void setMedalCutoff() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Jsonb jsonb = JsonbBuilder.create();
    Tournament tournament = jsonb.fromJson(tournamentJson, Tournament.class);
    Event jvOI = new Event();
    jvOI.setName("Junior Varsity Oral Interpretation");
    jvOI.setTournament(tournament);
    tournament.events = Collections.singletonList(
        jvOI
    );
    entityManager.persist(tournament);
    transaction.commit();

    given()
        .queryParam("eventId", jvOI.getId())
        .queryParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .when()
        .post("/results")
        .then()
        .statusCode(200);

    given()
        .pathParam("id", tournament.getId())
        .pathParam("evtId", jvOI.getId())
        .body("{\"cutoff\":\"3\"}")
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .post("/tournaments/{id}/events/{evtId}/medal")
        .then()
        .statusCode(200);

    Event jvOIAfter = entityManager.find(Event.class, jvOI.getId());
    assertThat(jvOIAfter.getMedalCutoff(), CoreMatchers.is(3));

  }

  @Test
  void generateCertificates() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Jsonb jsonb = JsonbBuilder.create();
    Tournament tournament = jsonb.fromJson(tournamentJson, Tournament.class);
    Event jvOI = new Event();
    jvOI.setName("Junior Varsity Oral Interpretation");
    jvOI.setTournament(tournament);
    jvOI.setCertificateCutoff(9);
    jvOI.setPlacementCutoff(6);

    Event duo = new Event();
    duo.setName("Duo Interpretation");
    duo.setTournament(tournament);
    duo.setCertificateCutoff(3);
    duo.setPlacementCutoff(3);
    tournament.events = Arrays.asList(
        jvOI,
        duo
    );
    entityManager.persist(tournament);
    transaction.commit();

    given()
        .queryParam("eventId", jvOI.getId())
        .queryParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .when()
        .post("/results");

    given()
        .queryParam("eventId", duo.getId())
        .queryParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/duo.csv"))
        .when()
        .post("/results");

    String certificateHtml = given()
        .pathParam("id", tournament.getId())
        .get("/tournaments/{id}/certificates")
        .body()
        .asString();


    assertThat(certificateHtml, containsString("Finalist"));
    assertThat(certificateHtml, containsString("Fifth Place"));
    assertThat(certificateHtml, containsString("First Place"));
    assertThat(certificateHtml, containsString("Leticia Irving"));
    assertThat(certificateHtml, not(containsString("River Weaver")));
    assertThat(certificateHtml, MultiStringMatcher.containsStringNTimes("Junior Varsity Oral Interpretation", 8));
    assertThat(certificateHtml, MultiStringMatcher.containsStringNTimes("Duo Interpretation", 2));
  }

  @Test
  void getMedalCount() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Jsonb jsonb = JsonbBuilder.create();
    Tournament tournament = jsonb.fromJson(tournamentJson, Tournament.class);
    Event jvOI = new Event();
    jvOI.setName("Junior Varsity Oral Interpretation");
    jvOI.setTournament(tournament);
    jvOI.setCertificateCutoff(9);
    jvOI.setPlacementCutoff(6);
    jvOI.setMedalCutoff(4);

    Event duo = new Event();
    duo.setName("Duo Interpretation");
    duo.setTournament(tournament);
    duo.setCertificateCutoff(3);
    duo.setPlacementCutoff(3);
    duo.setMedalCutoff(4);
    tournament.events = Arrays.asList(
        jvOI,
        duo
    );
    entityManager.persist(tournament);
    transaction.commit();

    given()
        .queryParam("eventId", jvOI.getId())
        .queryParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .when()
        .post("/results");

    given()
        .queryParam("eventId", duo.getId())
        .queryParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/duo.csv"))
        .when()
        .post("/results");

    List<MedalCount> medalCounts = given()
        .pathParam("id", tournament.getId())
        .get("/tournaments/{id}/medals")
        .body()
        .as(
            new ArrayList<MedalCount>() {
            }.getClass().getGenericSuperclass()
        );

    assertThat(medalCounts, hasItems(
        new MedalCount("Regis", 3, 0),
        new MedalCount("Bronx Science", 1, 0),
        new MedalCount("Convent of the Sacred Heart", 1, 0)
    ));
  }
}