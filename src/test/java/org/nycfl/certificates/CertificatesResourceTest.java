package org.nycfl.certificates;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.oidc.server.OidcWiremockTestResource;
import io.restassured.RestAssured;
import io.restassured.config.ObjectMapperConfig;
import io.restassured.mapper.ObjectMapper;
import io.restassured.mapper.ObjectMapperDeserializationContext;
import io.restassured.mapper.ObjectMapperSerializationContext;
import io.restassured.specification.RequestSpecification;
import io.smallrye.jwt.build.Jwt;
import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.transaction.*;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;

@QuarkusTest
@TestHTTPEndpoint(CertificatesResource.class)
@QuarkusTestResource(OidcWiremockTestResource.class)
class CertificatesResourceTest {
  @Inject
  EntityManager entityManager;

  @Inject
  UserTransaction transaction;

  static Jsonb jsonb;

  private String getToken(Set<String> roles) {
    return Jwt.preferredUserName("frank")
        .groups(roles)
        .issuer("https://server.example.com")
        .audience("https://service.example.com")
        .jws()
        .keyId("1")
        .sign();
  }

  @BeforeAll
  public static void giveMeAMapper() {
    jsonb = JsonbBuilder.create();
    ObjectMapper mapper = new ObjectMapper() {
      public Object deserialize(ObjectMapperDeserializationContext context) {
        return jsonb.fromJson(context.getDataToDeserialize().asString(), context.getType());
      }

      public Object serialize(ObjectMapperSerializationContext context) {
        return jsonb.toJson(context.getObjectToSerialize());
      }
    };
    RestAssured.config = RestAssured.config().objectMapperConfig(
            ObjectMapperConfig.objectMapperConfig().defaultObjectMapper(mapper)
    );
  }

  @AfterAll
  public static void releaseMapper() throws Exception {
    jsonb.close();
  }

  @AfterEach
  void cleanUp() throws SystemException, NotSupportedException,
          HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    List<Tournament> tournaments = entityManager
            .createQuery("SELECT t FROM Tournament t ", Tournament.class)
            .getResultList();
    tournaments.forEach(t->entityManager.remove(t));
    transaction.commit();
  }

  @Test
  void createTournament() {

    Long numTourneysBefore = entityManager
        .createQuery("SELECT COUNT(distinct t.id) FROM Tournament t", Long.class)
        .getSingleResult();

    givenASuperUser()
        .body("{\n" +
            "              \"name\": \"NYCFL First Regis\",\n" +
            "              \"host\": \"Regis High School\",\n" +
            "              \"date\": \"2020-09-26\"\n" +
            "            }" +
            "")
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
  void testUpdateTournament() throws HeuristicRollbackException,
          RollbackException, HeuristicMixedException, SystemException, NotSupportedException {
    transaction.begin();
    Tournament tournament = testTournament();
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
            .body("{\n" +
                    "  \"name\": \"Byram Hills Invitational\",\n" +
                    "  \"host\": \"Byram Hills " +
                    "High School\",\n" +
                    "  \"date\": \"2020-10-10\",\n" +
                    "  \"logoUrl\": \"https://s3.amazonaws.com/tabroom-files/tourns/16385/ByramBobcat.JPG\",\n" +
                    "  \"certificateHeadline\": \"Byram Hills Invitational Tournament\",\n" +
                    "  \"signature\": \"Someone Else\"\n" +
                    "}")
            .contentType(MediaType.APPLICATION_JSON)
            .pathParam("id", tournament.getId())
            .when()
            .post("/tournaments/{id}")
            .then()
            .statusCode(200);

    Tournament tournamentAfterTest = entityManager.find(Tournament.class,
            tournament.getId());

    assertThat(tournamentAfterTest.getLogoUrl(), CoreMatchers.is("https://s3.amazonaws.com/tabroom-files/tourns/16385/ByramBobcat.JPG"));

  }
  @Test
  void testCreateEvents() throws HeuristicRollbackException, RollbackException, HeuristicMixedException, SystemException, NotSupportedException {
    transaction.begin();
    Tournament tournament = testTournament();
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
        .body(String.format("{\"tournamentId\":\"%d\",\"events\":\"Junior " +
                                 "Varsity Oralnterpretation\\nDuo " +
            "Interpretation\"}", tournament.getId()))
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
  void testRequiresSuperuser() throws HeuristicRollbackException, RollbackException, HeuristicMixedException, SystemException, NotSupportedException {
    transaction.begin();
    Tournament tournament = testTournament();
    entityManager.persist(tournament);
    transaction.commit();

    givenARegularUser()
        .body(String.format("{\"tournamentId\":\"%d\",\"events\":\"Junior " +
                                 "Varsity Oralnterpretation\\nDuo " +
            "Interpretation\"}", tournament.getId()))
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .post("/events")
        .then()
        .statusCode(403);
  }
  @Test
  void testRequiresAuth() throws HeuristicRollbackException, RollbackException, HeuristicMixedException, SystemException, NotSupportedException {
    transaction.begin();
    Tournament tournament = testTournament();
    entityManager.persist(tournament);
    transaction.commit();

    given()
        .body(String.format("{\"tournamentId\":\"%d\",\"events\":\"Junior " +
                                 "Varsity Oralnterpretation\\nDuo " +
            "Interpretation\"}", tournament.getId()))
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .post("/events")
        .then()
        .statusCode(401);
  }

  @Test
  void addSpeechResults() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Tournament tournament = testTournament();
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

    givenASuperUser()
        .pathParam("eventId", jvOI.getId())
        .pathParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .when()
        .post("/tournaments/{tournamentId}/events/{eventId}/results")
        .then()
        .statusCode(200);

    Long numResults = entityManager
        .createQuery("SELECT COUNT(distinct r.id) FROM Result r WHERE r.event.id = ?1 and r.event.tournament.id=?2", Long.class)
        .setParameter(1, jvOI.getId())
        .setParameter(2, tournament.getId())
        .getSingleResult();

    assertThat(numResults, CoreMatchers.is(12L));

    givenASuperUser()
        .pathParam("eventId", duo.getId())
        .pathParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/duo.csv"))
        .when()
        .post("/tournaments/{tournamentId}/events/{eventId}/results")
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
  void addLDResults() throws SystemException, NotSupportedException,
          HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Tournament tournament = testTournament();
    Event lincolnDouglas = new Event();
    lincolnDouglas.setName("Lincoln Douglas Debate");
    lincolnDouglas.setTournament(tournament);
    lincolnDouglas.setEventType(EventType.DEBATE_LD);
    tournament.events = Collections.singletonList(
            lincolnDouglas
    );
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
            .queryParam("type",EliminationRound.QUARTER_FINALIST.name())
            .pathParam("eventId", lincolnDouglas.getId())
            .pathParam("tournamentId", tournament.getId())
            .multiPart(new File("src/test/resources/ld-quarters.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

    givenASuperUser()
            .queryParam("type",EliminationRound.SEMIFINALIST.name())
            .pathParam("eventId", lincolnDouglas.getId())
            .pathParam("tournamentId", tournament.getId())
            .multiPart(new File("src/test/resources/ld-semis.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

    givenASuperUser()
            .queryParam("type",EliminationRound.FINALIST.name())
            .pathParam("eventId", lincolnDouglas.getId())
            .pathParam("tournamentId", tournament.getId())
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
            ()->assertThat(quarterFinalist.eliminationRound,
                    is(EliminationRound.QUARTER_FINALIST)),
            ()->assertThat(semiFinalist.eliminationRound,
                    is(EliminationRound.SEMIFINALIST)),
            ()->assertThat(finalist.eliminationRound,
                    is(EliminationRound.FINALIST))
    );
  }
  @Test
  void addPFResults() throws SystemException, NotSupportedException,
          HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Tournament tournament = testTournament();
    Event lincolnDouglas = new Event();
    lincolnDouglas.setName("Public Forum Debate");
    lincolnDouglas.setTournament(tournament);
    lincolnDouglas.setEventType(EventType.DEBATE_PF);
    tournament.events = Collections.singletonList(
            lincolnDouglas
    );
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
            .queryParam("type",EliminationRound.QUARTER_FINALIST.name())
            .pathParam("eventId", lincolnDouglas.getId())
            .pathParam("tournamentId", tournament.getId())
            .multiPart(new File("src/test/resources/pf-quarters.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

    givenASuperUser()
            .queryParam("type",EliminationRound.SEMIFINALIST.name())
            .pathParam("eventId", lincolnDouglas.getId())
            .pathParam("tournamentId", tournament.getId())
            .multiPart(new File("src/test/resources/pf-semis.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

    givenASuperUser()
            .queryParam("type",EliminationRound.FINALIST.name())
            .pathParam("eventId", lincolnDouglas.getId())
            .pathParam("tournamentId", tournament.getId())
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
            ()->assertThat(quarterFinalist.eliminationRound,
                    is(EliminationRound.QUARTER_FINALIST)),
            ()->assertThat(semiFinalist.eliminationRound,
                    is(EliminationRound.SEMIFINALIST)),
            ()->assertThat(finalist.eliminationRound,
                    is(EliminationRound.FINALIST))
    );
  }

  @Test
  void addDebateSpeaks() throws SystemException, NotSupportedException,
          HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Tournament tournament = testTournament();
    Event lincolnDouglas = new Event();
    lincolnDouglas.setName("Public Forum Debate Speaker Awards");
    lincolnDouglas.setTournament(tournament);
    lincolnDouglas.setEventType(EventType.DEBATE_SPEAKS);
    tournament.events = Collections.singletonList(
            lincolnDouglas
    );
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
            .pathParam("eventId", lincolnDouglas.getId())
            .pathParam("tournamentId", tournament.getId())
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

    assertThat(topSpeaks.place, is(1));
  }

  @Test
  void testAddSweeps() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Tournament tournament = testTournament();
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
        .pathParam("id", tournament.getId())
        .multiPart(new File("src/test/resources/schools.csv"))
        .post("/tournaments/{id}/schools")
        .body();
    givenASuperUser()
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
  void testDeleteSchoolWithoutResults() throws SystemException,
      NotSupportedException,
      HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Tournament tournament = testTournament();
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
        .pathParam("id", tournament.getId())
        .multiPart(new File("src/test/resources/schools.csv"))
        .post("/tournaments/{id}/schools")
        .body();

    Long regisId = entityManager
        .createQuery("select s.id FROM School s WHERE s.name = ?1 and s" +
                ".tournament.id = ?2",
            Long.class)
        .setParameter(1, "Regis")
        .setParameter(2, tournament.getId())
        .getSingleResult();

    givenASuperUser()
        .pathParam("id", tournament.getId())
        .pathParam("sid", regisId)
        .when()
        .delete("/tournaments/{id}/schools/{sid}")
        .then().statusCode(200);

    assertThrows(NoResultException.class, () ->
        entityManager
            .createQuery("select s.id FROM School s WHERE s.name = ?1 and s" +
                    ".tournament.id = ?2",
                Long.class)
            .setParameter(1, "Regis")
            .setParameter(2, tournament.getId())
            .getSingleResult());
  }
  @Test
  void testCannotDeleteSchoolWithResults() throws SystemException,
      NotSupportedException,
      HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Tournament tournament = testTournament();

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

    givenASuperUser()
        .pathParam("id", tournament.getId())
        .multiPart(new File("src/test/resources/schools.csv"))
        .post("/tournaments/{id}/schools")
        .body();

    givenASuperUser()
        .pathParam("eventId", jvOI.getId())
        .pathParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .when()
        .post("/tournaments/{tournamentId}/events/{eventId}/results");

    Long regisId = entityManager
        .createQuery("select s.id FROM School s WHERE s.name = ?1 and s" +
                ".tournament.id = ?2",
            Long.class)
        .setParameter(1, "Regis")
        .setParameter(2, tournament.getId())
        .getSingleResult();

    givenASuperUser()
        .pathParam("id", tournament.getId())
        .pathParam("sid", regisId)
        .when()
        .delete("/tournaments/{id}/schools/{sid}")
        .then().statusCode(HttpStatus.SC_BAD_REQUEST);
  }

  @Test
  void testGetOneTournamentSweeps() throws SystemException,
          NotSupportedException,
          HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Tournament tournament = testTournament();
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
        .pathParam("id", tournament.getId())
        .multiPart(new File("src/test/resources/schools.csv"))
        .post("/tournaments/{id}/schools")
        .body();
    givenASuperUser()
        .pathParam("id", tournament.getId())
        .multiPart(new File("src/test/resources/sweeps.csv"))
        .post("/tournaments/{id}/sweeps")
        .body();

    List<SweepsResult> sweepsResults = givenARegularUser()
            .pathParam("id", tournament.getId())
            .get("/tournaments/{id}/sweeps")
            .body()
            .as(new ArrayList<SweepsResult>() {
                    }.getClass().getGenericSuperclass()
            );

    assertThat(sweepsResults, hasSize(13));
  }
  @Test
  void testGetTwoTournamentSweeps() throws SystemException,
          NotSupportedException,
          HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Tournament tournament1 = testTournament();
    Tournament tournament2 = jsonb.fromJson("{\n" +
        "          \"name\": \"NYCFL Hugh McEvoy\",\n" +
        "          \"host\": \"Stuyvesant High School\",\n" +
        "          \"date\": \"2020-10-03\"\n" +
        "        }", Tournament.class);
    entityManager.persist(tournament1);
    entityManager.persist(tournament2);
    transaction.commit();

    givenASuperUser()
        .pathParam("id", tournament1.getId())
        .multiPart(new File("src/test/resources/schools.csv"))
        .post("/tournaments/{id}/schools")
        .body();
    givenASuperUser()
        .pathParam("id", tournament1.getId())
        .multiPart(new File("src/test/resources/sweeps.csv"))
        .post("/tournaments/{id}/sweeps")
        .body();
    givenASuperUser()
        .pathParam("id", tournament2.getId())
        .multiPart(new File("src/test/resources/schools.csv"))
        .post("/tournaments/{id}/schools")
        .body();
    givenASuperUser()
        .pathParam("id", tournament2.getId())
        .multiPart(new File("src/test/resources/sweeps2.csv"))
        .post("/tournaments/{id}/sweeps")
        .body();

    AggregateSweeps sweepsResults = givenARegularUser()
            .get("/tournaments/sweeps")
            .body()
            .as(AggregateSweeps.class);

    assertThat(sweepsResults.totals.get("Regis"), is(82+89));
    assertThat(sweepsResults.totals.get("Convent of the Sacred Heart, NYC"),
            is(97+79));
    assertThat(sweepsResults.totals.get("Democracy Prep Harlem Prep"),
            is(39+13));
  }

  private RequestSpecification givenASuperUser() {
    return given()
        .auth()
        .oauth2(getToken(Collections.singleton("superuser")));
  }

  private RequestSpecification givenARegularUser() {
    return given()
        .auth()
        .oauth2(getToken(Collections.singleton("user")));
  }

  private Tournament testTournament() {
    return jsonb.fromJson("" +
        "{\n" +
        "          \"name\": \"NYCFL First Regis\",\n" +
        "          \"host\": \"Regis High School\",\n" +
        "          \"date\": \"2020-09-26\"\n" +
        "        }", Tournament.class);
  }

  @Test
  void listAllTournaments() {
    Long numTourneys = entityManager
        .createQuery("SELECT COUNT(t.id) FROM Tournament t", Long.class)
        .getSingleResult();

    List<Tournament> tournaments = givenARegularUser()
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
    Tournament tournament = testTournament();
    Event jvOI = new Event();
    jvOI.setName("Junior Varsity Oral Interpretation");
    jvOI.setTournament(tournament);
    tournament.events = Collections.singletonList(
        jvOI
    );
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
        .pathParam("eventId", jvOI.getId())
        .pathParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .when()
        .post("/tournaments/{tournamentId}/events/{eventId}/results");

    List<School> schools = givenARegularUser()
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
    Tournament tournament = testTournament();
    entityManager.persist(tournament);
    transaction.commit();

    String schoolsJson = givenASuperUser()
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
  void clearResults() throws SystemException, NotSupportedException,
      HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Tournament tournament = testTournament();
    Event jvOI = new Event();
    jvOI.setName("Junior Varsity Oral Interpretation");
    jvOI.setTournament(tournament);
    tournament.events = Collections.singletonList(
        jvOI
    );
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
        .pathParam("eventId", jvOI.getId())
        .pathParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .post("/tournaments/{tournamentId}/events/{eventId}/results");

    givenASuperUser()
        .pathParam("eventId", jvOI.getId())
        .pathParam("tournamentId", tournament.getId())
        .delete("/tournaments/{tournamentId}/events/{eventId}/results");

    Event jvOIAfter = entityManager.find(Event.class, jvOI.getId());
    assertThat(jvOIAfter.getResults(), hasSize(0));
  }

  @Test
  void renameResult() throws SystemException, NotSupportedException,
      HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Tournament tournament = testTournament();
    Event jvOI = new Event();
    jvOI.setName("Junior Varsity Oral Interpretation");
    jvOI.setTournament(tournament);
    tournament.events = Collections.singletonList(
        jvOI
    );
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
        .pathParam("eventId", jvOI.getId())
        .pathParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .post("/tournaments/{tournamentId}/events/{eventId}/results");

    Long
        resultId =
        entityManager
            .createQuery("Select id FROM Result r WHERE r.name = 'Carina" +
                " " +
                "Dillard'", Long.class).getSingleResult();

    givenASuperUser()
        .pathParam("evtId", jvOI.getId())
        .pathParam("id", tournament.getId())
        .pathParam("resultId", resultId)
        .queryParam("name", "Johnny Newname")
        .contentType(MediaType.APPLICATION_JSON)
        .post("/tournaments/{id}/events/{evtId}/results/{resultId}/rename")
        .then()
        .statusCode(200);;

    Result result = entityManager.find(Result.class, resultId);

    assertThat(result.name, is("Johnny Newname"));
  }

  @Test
  void setPlacementCutoff() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Tournament tournament = testTournament();
    Event jvOI = new Event();
    jvOI.setName("Junior Varsity Oral Interpretation");
    jvOI.setTournament(tournament);
    tournament.events = Collections.singletonList(
        jvOI
    );
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
        .pathParam("eventId", jvOI.getId())
        .pathParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .post("/tournaments/{tournamentId}/events/{eventId}/results");

    givenASuperUser()
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
    Tournament tournament = testTournament();
    Event jvOI = new Event();
    jvOI.setName("Junior Varsity Oral Interpretation");
    jvOI.setTournament(tournament);
    tournament.events = Collections.singletonList(
        jvOI
    );
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
        .pathParam("eventId", jvOI.getId())
        .pathParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .when()
        .post("/tournaments/{tournamentId}/events/{eventId}/results")
        .then()
        .statusCode(200);

    givenASuperUser()
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
  void changeEventType() throws SystemException, NotSupportedException,
          HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Tournament tournament = testTournament();
    Event lincolnDouglas = new Event();
    lincolnDouglas.setName("Lincoln-Douglas Debate");
    lincolnDouglas.setTournament(tournament);
    tournament.events = Collections.singletonList(
        lincolnDouglas
    );
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
        .pathParam("eventId", lincolnDouglas.getId())
        .pathParam("tournamentId", tournament.getId())
        .queryParam("type", EventType.DEBATE_LD.name())
            .contentType(MediaType.APPLICATION_JSON)
        .when()
        .post("/tournaments/{tournamentId}/events/{eventId}/type")
        .then()
        .statusCode(200);

    Event ldAfter = entityManager.find(Event.class, lincolnDouglas.getId());
    assertThat(ldAfter.getEventType(), CoreMatchers.is(EventType.DEBATE_LD));

  }  @Test
  void createSpeakerAwards() throws SystemException, NotSupportedException,
          HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Tournament tournament = testTournament();
    Event lincolnDouglas = new Event();
    lincolnDouglas.setName("Lincoln-Douglas Debate");
    lincolnDouglas.setTournament(tournament);
    tournament.events = Collections.singletonList(
        lincolnDouglas
    );
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
        .pathParam("eventId", lincolnDouglas.getId())
        .pathParam("tournamentId", tournament.getId())
        .queryParam("type", EventType.DEBATE_LD.name())
            .contentType(MediaType.APPLICATION_JSON)
        .when()
        .post("/tournaments/{tournamentId}/events/{eventId}/type")
        .then()
        .statusCode(200);

    List<Event> ldAfter = entityManager.createQuery("SELECT e FROM Event e WHERE e" +
        ".eventType='DEBATE_SPEAKS'", Event.class).getResultList();
    assertThat(ldAfter, hasSize(1));

  }

  @Test
  void setMedalCutoff() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
    transaction.begin();
    Tournament tournament = testTournament();
    Event jvOI = new Event();
    jvOI.setName("Junior Varsity Oral Interpretation");
    jvOI.setTournament(tournament);
    tournament.events = Collections.singletonList(
        jvOI
    );
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
        .pathParam("eventId", jvOI.getId())
        .pathParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .when()
        .post("/tournaments/{tournamentId}/events/{eventId}/results")
        .then()
        .statusCode(200);

    givenASuperUser()
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
    Tournament tournament = testTournament();
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

    givenASuperUser()
        .pathParam("eventId", jvOI.getId())
        .pathParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .when()
        .post("/tournaments/{tournamentId}/events/{eventId}/results");

    givenASuperUser()
        .pathParam("eventId", duo.getId())
        .pathParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/duo.csv"))
        .when()
        .post("/tournaments/{tournamentId}/events/{eventId}/results");

    String certificateHtml = givenARegularUser()
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
    Tournament tournament = testTournament();
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

    givenASuperUser()
        .pathParam("eventId", jvOI.getId())
        .pathParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/JV-OI.csv"))
        .when()
        .post("/tournaments/{tournamentId}/events/{eventId}/results");

    givenASuperUser()
        .pathParam("eventId", duo.getId())
        .pathParam("tournamentId", tournament.getId())
        .multiPart(new File("src/test/resources/duo.csv"))
        .when()
        .post("/tournaments/{tournamentId}/events/{eventId}/results");

    List<MedalCount> medalCounts = givenARegularUser()
        .pathParam("id", tournament.getId())
        .get("/tournaments/{id}/medals")
        .body()
        .as(
            new ArrayList<MedalCount>() {
            }.getClass().getGenericSuperclass()
        );

    assertThat(medalCounts, hasItems(
        new MedalCount("Regis", 5),
        new MedalCount("Bronx Science", 1),
        new MedalCount("Convent of the Sacred Heart", 1)
    ));
  }
}