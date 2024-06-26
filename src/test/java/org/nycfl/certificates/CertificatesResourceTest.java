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
import org.apache.http.HttpStatus;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.*;
import org.nycfl.certificates.results.Result;

import jakarta.inject.Inject;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import jakarta.transaction.*;
import jakarta.ws.rs.core.MediaType;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.BAD_REQUEST;
import static org.jboss.resteasy.reactive.RestResponse.StatusCode.NOT_FOUND;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.nycfl.certificates.TestUtils.givenARegularUser;
import static org.nycfl.certificates.TestUtils.givenASuperUser;

@QuarkusTest
@TestHTTPEndpoint(CertificatesResource.class)
@QuarkusTestResource(OidcWiremockTestResource.class)
class CertificatesResourceTest {
    @Inject
    EntityManager entityManager;

    @Inject
    UserTransaction transaction;

    static Jsonb jsonb;

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

        assertThat(numTourneysAfter, CoreMatchers.is(numTourneysBefore + 1));
    }
    @Test
    void getAllTournaments() {


        givenASuperUser()
            .body("""
                {
                              "name": "NYCFL First Regis",
                              "host": "Regis High School",
                              "date": "2020-09-26"
                            }""")
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/tournaments");

        givenASuperUser()
            .body("""
                {
                              "name": "NYCFL Sr. Raimonde Memorial",
                              "host": "Xavier High School",
                              "date": "2020-10-26"
                            }""")
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/tournaments");

        givenASuperUser()
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .get("/tournaments")
            .then()
            .statusCode(200)
            .body("$.size()", equalTo(2));


    }

    @Test
    void testUpdateTournament() throws HeuristicRollbackException,
        RollbackException, HeuristicMixedException, SystemException, NotSupportedException {
        transaction.begin();
        Tournament tournament = testTournament();
        entityManager.persist(tournament);
        transaction.commit();

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
    void testGetTournament() throws HeuristicRollbackException,
        RollbackException, HeuristicMixedException, SystemException, NotSupportedException {
        transaction.begin();
        Tournament tournament = testTournament();
        entityManager.persist(tournament);
        transaction.commit();

        givenASuperUser()
            .contentType(MediaType.APPLICATION_JSON)
            .pathParam("id", tournament.getId())
            .when()
            .get("/tournaments/{id}")
            .then()
            .statusCode(200)
            .body("name", equalTo("NYCFL First Regis"));
    }

  @Test
  @DisplayName("Clone a tournament that has been customized")
  void testCloneTournament() throws HeuristicRollbackException,
          RollbackException, HeuristicMixedException, SystemException, NotSupportedException {
    transaction.begin();
    Tournament tournament = testTournament();
    entityManager.persist(tournament);
    transaction.commit();

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
            .pathParam("id", tournament.getId())
            .when()
            .post("/tournaments/{id}")
            .then()
            .statusCode(200);

    givenASuperUser()
        .contentType(MediaType.APPLICATION_JSON)
        .queryParam("sourceId", tournament.getId())
        .when()
        .post("/tournaments")
        .then()
        .statusCode(200)
        .body(
            CoreMatchers.containsString("Copy of Byram Hills Invitational"),
            CoreMatchers.containsString("Someone Else")
        );
  }
  @Test
  @DisplayName("Clone a tournament that has not been customized")
  void testCloneTournament2() throws HeuristicRollbackException,
          RollbackException, HeuristicMixedException, SystemException, NotSupportedException {
    transaction.begin();
    Tournament tournament = testTournament();
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
        .contentType(MediaType.APPLICATION_JSON)
        .queryParam("sourceId", tournament.getId())
        .when()
        .post("/tournaments")
        .then()
        .statusCode(200)
        .body(
            CoreMatchers.containsString("Copy of " + tournament.getName())
        );
  }
  @Test
  @DisplayName("Cannot clone a tournament with an invalid ID")
  void testBadCloneTournament() throws HeuristicRollbackException,
          RollbackException, HeuristicMixedException, SystemException, NotSupportedException {
    transaction.begin();
    Tournament tournament = testTournament();
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
        .contentType(MediaType.APPLICATION_JSON)
        .queryParam("sourceId", 999)
        .when()
        .post("/tournaments")
        .then()
        .statusCode(NOT_FOUND);
  }

  @Test
  @DisplayName("Cannot call create tournament with no sourceID or payload")
  void testBadCloneTournament2() throws HeuristicRollbackException,
          RollbackException, HeuristicMixedException, SystemException, NotSupportedException {
    transaction.begin();
    Tournament tournament = testTournament();
    entityManager.persist(tournament);
    transaction.commit();

    givenASuperUser()
        .contentType(MediaType.APPLICATION_JSON)
        .when()
        .post("/tournaments")
        .then()
        .statusCode(BAD_REQUEST);
  }
  @Test
  void testCreateEvents() throws HeuristicRollbackException, RollbackException, HeuristicMixedException, SystemException, NotSupportedException {
    transaction.begin();
    Tournament tournament = testTournament();
    entityManager.persist(tournament);
    transaction.commit();

        givenASuperUser()
            .body(String.format("{\"tournamentId\":\"%d\",\"events\":\"Junior Varsity Oral Interpretation\\nDuo Interpretation\"}", tournament.getId()))
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
    void testAbbreviateEvent() throws HeuristicRollbackException, RollbackException,
        HeuristicMixedException, SystemException, NotSupportedException {
        transaction.begin();
        Tournament tournament = testTournament();
        entityManager.persist(tournament);
        transaction.commit();

        givenASuperUser()
            .body(String.format("{\"tournamentId\":\"%d\",\"events\":\"Junior " +
                "Varsity Oral Interpretation\\nDuo " +
                "Interpretation\"}", tournament.getId()))
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/events");

        Long evtId = entityManager
            .createQuery("SELECT e.id FROM Event e WHERE e.tournament.id=?1",
                Long.class)
            .setMaxResults(1)
            .setParameter(1, tournament.getId())
            .getSingleResult();


        givenASuperUser()
            .queryParam("abbreviation", "JV OI")
            .pathParam("id", tournament.getId())
            .pathParam("evtId", evtId)
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/tournaments/{id}/events/{evtId}/abbreviate")
            .then()
            .statusCode(200)
            .body(CoreMatchers.containsString("JV OI"));

        String abbreviation = entityManager
            .createQuery("SELECT e.abbreviation FROM Event e WHERE e.id=?1", String.class)
            .setParameter(1, evtId)
            .getSingleResult();

        assertThat(abbreviation, CoreMatchers.is("JV OI"));

    }

    @Test
    void testRequiresSuperuser() throws HeuristicRollbackException, RollbackException, HeuristicMixedException, SystemException, NotSupportedException {
        transaction.begin();
        Tournament tournament = testTournament();
        entityManager.persist(tournament);
        transaction.commit();

        givenARegularUser()
            .body(String.format("{\"tournamentId\":\"%d\",\"events\":\"Junior " +
                "Varsity Oral Interpretation\\nDuo " +
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
                "Varsity Oral Interpretation\\nDuo " +
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
            .queryParam("type", EliminationRound.QUARTER_FINALIST.name())
            .pathParam("eventId", lincolnDouglas.getId())
            .pathParam("tournamentId", tournament.getId())
            .multiPart(new File("src/test/resources/ld-quarters.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

        givenASuperUser()
            .queryParam("type", EliminationRound.SEMIFINALIST.name())
            .pathParam("eventId", lincolnDouglas.getId())
            .pathParam("tournamentId", tournament.getId())
            .multiPart(new File("src/test/resources/ld-semis.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

        givenASuperUser()
            .queryParam("type", EliminationRound.FINALIST.name())
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
            () -> assertThat(
                quarterFinalist.getEliminationRound(),
                is(EliminationRound.QUARTER_FINALIST)),
            () -> assertThat(
                semiFinalist.getEliminationRound(),
                is(EliminationRound.SEMIFINALIST)),
            () -> assertThat(finalist.getEliminationRound(),
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
            .queryParam("type", EliminationRound.QUARTER_FINALIST.name())
            .pathParam("eventId", lincolnDouglas.getId())
            .pathParam("tournamentId", tournament.getId())
            .multiPart(new File("src/test/resources/pf-quarters.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

        givenASuperUser()
            .queryParam("type", EliminationRound.SEMIFINALIST.name())
            .pathParam("eventId", lincolnDouglas.getId())
            .pathParam("tournamentId", tournament.getId())
            .multiPart(new File("src/test/resources/pf-semis.csv"))
            .when()
            .post("/tournaments/{tournamentId}/events/{eventId}/results")
            .then()
            .statusCode(200);

        givenASuperUser()
            .queryParam("type", EliminationRound.FINALIST.name())
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
            () -> assertThat(
                quarterFinalist.getEliminationRound(),
                is(EliminationRound.QUARTER_FINALIST)),
            () -> assertThat(
                semiFinalist.getEliminationRound(),
                is(EliminationRound.SEMIFINALIST)),
            () -> assertThat(finalist.getEliminationRound(),
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

        assertThat(topSpeaks.getPlace(), is(1));
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

        TypedQuery<Long> query = entityManager
            .createQuery("select s.id FROM School s WHERE s.name = ?1 and s" +
                    ".tournament.id = ?2",
                Long.class)
            .setParameter(1, "Regis")
            .setParameter(2, tournament.getId());
        assertThrows(NoResultException.class, query::getSingleResult);
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
        Tournament tournament2 = jsonb.fromJson("""
            {
                      "name": "NYCFL Hugh McEvoy",
                      "host": "Stuyvesant High School",
                      "date": "2020-10-03"
                    }""", Tournament.class);
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

        assertThat(sweepsResults.totals.get("Regis"), is(82 + 89));
        assertThat(sweepsResults.totals.get("Convent of the Sacred Heart, NYC"),
            is(97 + 79));
        assertThat(sweepsResults.totals.get("Democracy Prep Harlem Prep"),
            is(39 + 13));
    }

    private Tournament testTournament() {
        return jsonb.fromJson("""
            {
                      "name": "NYCFL First Regis",
                      "host": "Regis High School",
                      "date": "2020-09-26"
                    }""", Tournament.class);
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
                .createQuery(
                    "Select id FROM Result r WHERE r.name = 'Carina Dillard'",
                    Long.class
                ).getSingleResult();

        givenASuperUser()
            .pathParam("evtId", jvOI.getId())
            .pathParam("id", tournament.getId())
            .pathParam("resultId", resultId)
            .queryParam("name", "Johnny Newname")
            .contentType(MediaType.APPLICATION_JSON)
            .post("/tournaments/{id}/events/{evtId}/results/{resultId}/rename")
            .then()
            .statusCode(200);

        Result result = entityManager.find(Result.class, resultId);

        assertThat(result.getName(), is("Johnny Newname"));
    }

    @Test
    void renameResultCanFail() throws SystemException, NotSupportedException,
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
                .createQuery(
                    "Select id FROM Result r WHERE r.name = 'Carina Dillard'",
                    Long.class
                ).getSingleResult();

        givenASuperUser()
            .pathParam("evtId", 0)
            .pathParam("id", tournament.getId())
            .pathParam("resultId", resultId)
            .queryParam("name", "Johnny Newname")
            .contentType(MediaType.APPLICATION_JSON)
            .post("/tournaments/{id}/events/{evtId}/results/{resultId}/rename")
            .then()
            .statusCode(404);

        Result result = entityManager.find(Result.class, resultId);

        assertThat(result.getName(), is("Carina Dillard"));
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
    void setCertificateType() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
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
            .pathParam("id", tournament.getId())
            .pathParam("evtId", jvOI.getId())
            .queryParam("type", CertificateType.QUALIFIER)
            .contentType(MediaType.APPLICATION_JSON)
            .when()
            .post("/tournaments/{id}/events/{evtId}/cert_type")
            .then()
            .statusCode(200);

        Event jvOIAfter = entityManager.find(Event.class, jvOI.getId());
        assertThat(jvOIAfter.getCertificateType(), CoreMatchers.is(CertificateType.QUALIFIER));

    }

    @Test
    void setStateQualCutoff() throws SystemException, NotSupportedException, HeuristicRollbackException, HeuristicMixedException, RollbackException {
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
            .post("/tournaments/{id}/events/{evtId}/quals")
            .then()
            .statusCode(200);

        Event jvOIAfter = entityManager.find(Event.class, jvOI.getId());
        assertThat(jvOIAfter.getHalfQuals(), CoreMatchers.is(3));

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

    }

    @Test
    void deleteEvent() throws SystemException, NotSupportedException,
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
            .delete("/tournaments/{tournamentId}/events/{eventId}")
            .then()
            .statusCode(200);

        Event ldAfter = entityManager.find(Event.class, lincolnDouglas.getId());
        assertThat(ldAfter, CoreMatchers.nullValue());

    }

    @Test
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
        final Tournament testTournament = entityManager.find(Tournament.class, tournament.getId());
        final Map<String, Long> schoolMap = testTournament
            .schools
            .stream()
            .collect(
                Collectors.toMap(
                    School::getName,
                    School::getId
                )
            );
        assertThat(medalCounts, hasItems(
            new MedalCount("Regis", 5, schoolMap.get("Regis")),
            new MedalCount("Bronx Science", 1, schoolMap.get("Bronx Science")),
            new MedalCount("Convent of the Sacred Heart", 1, schoolMap.get("Convent of the Sacred Heart"))
        ));
    }
}