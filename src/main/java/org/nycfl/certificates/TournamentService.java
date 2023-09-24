package org.nycfl.certificates;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.jboss.logging.Logger;
import org.nycfl.certificates.results.Result;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@ApplicationScoped
public class TournamentService {
    private static final Logger LOG = Logger.getLogger(TournamentService.class);

    @Inject
    EntityManager em;

    @Transactional
    public Tournament createTournament(Tournament tournament) {
        em.persist(tournament);
        return tournament;
    }

    public List<TournamentStub> all() {
        return em.createQuery(
            """
            SELECT new org.nycfl.certificates.TournamentStub(t.id, t.name)
            FROM Tournament t
            ORDER BY t.tournamentDate desc
            """,
            TournamentStub.class).getResultList();
    }

    @Transactional
    public Tournament addEvents(EventList eventList) {
        Tournament tournament = em.createQuery(
                "SELECT t FROM Tournament t LEFT JOIN FETCH t.events WHERE t.id=?1",
                Tournament.class).setParameter(1, eventList.tournamentId())
            .getSingleResult();
        tournament.setEvents(eventList.getEvents());
        em.persist(tournament);
        return getTournament(tournament.getId());
    }

    @Transactional
    public Tournament addResults(
        long eventId,
        long tournamentId,
        EliminationRound eliminationRound,
        InputStream csvInputStream) {
        Event event = em.createQuery(
                "SELECT e FROM Event e LEFT JOIN FETCH e.tournament LEFT JOIN FETCH e.results WHERE e.id=?1",
                Event.class)
            .setParameter(1, eventId)
            .getSingleResult();

        Function<School, String> getName = event.getSchoolMappingFunction();
        List<School> schools = getSchools(tournamentId);
        Map<String, School> schoolsMap = schools
            .stream()
            .collect(
                Collectors.toMap(getName,
                    Function.identity()));

        event.parseResults(eliminationRound, csvInputStream, schoolsMap);
        addSchools(schoolsMap.values(), tournamentId);
        em.persist(event);
        return getTournament(event.getTournament().getId());
    }

    @Transactional
    public Tournament clearResults(long eventId) {
        Event event = em.createQuery(
                "SELECT e FROM Event e LEFT JOIN FETCH e.tournament LEFT JOIN FETCH e.results WHERE e.id=?1",
                Event.class)
            .setParameter(1, eventId)
            .getSingleResult();
        event.clearResults();
        em.persist(event);
        return getTournament(event.getTournament().getId());
    }

    public Tournament getTournament(Long tournamentId) {
        try {
            Tournament tournament = em.createQuery(
                    "SELECT DISTINCT t FROM Tournament t LEFT JOIN FETCH t.events e WHERE t.id=?1",
                    Tournament.class
                )
                .setParameter(1, tournamentId)
                .getSingleResult();

            em.createQuery(
                    """
                        SELECT DISTINCT e
                        FROM Event e
                        LEFT JOIN FETCH e.results
                        WHERE e.tournament=?1
                        """,
                    Event.class
                )
                .setParameter(1, tournament)
                .getResultList();
            return tournament;
        } catch (NoResultException nre) {
            throw new NotFoundException("Tournament " + tournamentId + " does not exist");
        }
    }

    @Transactional
    public void addSchools(Collection<School> schools, long tournamentId) {
        Tournament tournament = em.find(Tournament.class, tournamentId);
        tournament.addSchools(schools
            .stream()
            .filter(school -> school.getTournament() == null)
            .toList());
        em.persist(tournament);
    }

    public List<School> getSchools(long tournamentId) {
        return em.createQuery(
                """
                SELECT DISTINCT s
                FROM School s
                LEFT JOIN FETCH s.emails
                WHERE s.tournament.id=?1
                """,
                School.class).setParameter(1, tournamentId)
            .getResultList();
    }

    @Transactional
    public Tournament updatePlacementCutoff(long eventId, int cutoff) {
        Event event = em.find(Event.class, eventId);
        event.setPlacementCutoff(cutoff);
        em.persist(event);
        return getTournament(event.getTournament().getId());
    }

    @Transactional
    public Tournament updateCertificateCutoff(long eventId, int cutoff) {
        Event event = em.find(Event.class, eventId);
        event.setCertificateCutoff(cutoff);
        em.persist(event);
        return getTournament(event.getTournament().getId());
    }

    @Transactional
    public Tournament updateMedalCutoff(long eventId, int cutoff) {
        Event event = em.find(Event.class, eventId);
        event.setMedalCutoff(cutoff);
        em.persist(event);
        return getTournament(event.getTournament().getId());
    }

    @Transactional
    public Tournament updateHalfQuals(long eventId, int cutoff) {
        Event event = em.find(Event.class, eventId);
        event.setHalfQuals(cutoff);
        em.persist(event);
        return getTournament(event.getTournament().getId());
    }

    public List<MedalCount> getMedalCount(long tournamentId) {
        return em.createQuery(
                """
                SELECT new org.nycfl.certificates.MedalCount(
                r.school.name, sum(r.count), r.school.id)
                FROM Event e
                LEFT JOIN e.results r
                WHERE e.tournament.id = ?1
                AND r.place < e.medalCutoff
                GROUP BY r.school
                ORDER BY r.school.name
                """
                , MedalCount.class)
            .setParameter(1, tournamentId)
            .getResultList();
    }

    @Transactional
    public void updateSchool(School school, long tournamentId) {
        if (school.getId() == 0) {
            school.setTournament(em.find(Tournament.class, tournamentId));
            em.persist(school);
        } else {
            em.merge(school);
        }
    }

    public AggregateSweeps getSweeps() {
        return new AggregateSweeps(em.createQuery("SELECT new org.nycfl.certificates" +
                ".SweepsResult" +
                "(s.name, coalesce(s.sweepsPoints, 0),  t.name, t.id, s.id) " +
                "FROM School s " +
                "LEFT JOIN s.tournament t " +
                "ORDER BY s.name", SweepsResult.class)
            .getResultList());
    }

    public List<SweepsResult> getSweeps(long tournamentId) {
        return em.createQuery(
                """
                SELECT new org.nycfl.certificates.SweepsResult(s.name, coalesce(s.sweepsPoints, 0),  t.name, t.id, s.id)
                FROM School s
                LEFT JOIN s.tournament t
                WHERE t.id = ?1
                ORDER BY s.name
                """, SweepsResult.class)
            .setParameter(1, tournamentId)
            .getResultList();
    }

    @Transactional
    public Tournament updateTournament(
        long tournamentId,
        Tournament updatedTournament) {
        Tournament persistedTournament = getTournament(tournamentId);
        persistedTournament.merge(updatedTournament);
        em.persist(persistedTournament);
        return persistedTournament;
    }

    @Transactional
    public Tournament updateEventType(long eventId, EventType eventType) {
        Event event = em.find(Event.class, eventId);
        event.setEventType(eventType);
        if (eventType.hasSpeakerAwards()) {
            Event speakerEvent = new Event();
            speakerEvent.setEventType(EventType.DEBATE_SPEAKS);
            speakerEvent.setName(event.getName() + " Speaker Awards");
            speakerEvent.setCertificateType(CertificateType.DEBATE_SPEAKER);
            speakerEvent.setTournament(event.getTournament());
            em.persist(speakerEvent);
        }
        em.persist(event);
        return getTournament(event.getTournament().getId());
    }

    @Transactional
    public List<School> deleteSchool(long tournamentId, long schoolId) {
        int
            updated =
            em.createQuery(
                    "DELETE FROM School s WHERE s.id=?1 and s.tournament.id = ?2 and size(s.results) = 0")
                .setParameter(1, schoolId)
                .setParameter(2, tournamentId)
                .executeUpdate();
        if (updated != 1) {
            throw new BadRequestException("Could not delete school");
        }
        return em
            .createQuery("SELECT s FROM School s WHERE s.tournament.id = ?1", School.class)
            .setParameter(1, tournamentId)
            .getResultList();
    }


    @Transactional
    public Tournament updateCertificateType(long eventId,
                                            CertificateType certificateType) {
        Event event = em.find(Event.class, eventId);
        event.setCertificateType(certificateType);
        em.persist(event);
        return getTournament(event.getTournament().getId());
    }

    @Transactional
    public Tournament updateNumRounds(long eventId, int count) {
        Event event = em.find(Event.class, eventId);
        event.setNumRounds(count);
        em.persist(event);
        return getTournament(event.getTournament().getId());

    }

    @Transactional
    public Tournament renameEvent(long eventId, String newName) {
        Event event = em.find(Event.class, eventId);
        event.setName(newName);
        em.persist(event);
        return getTournament(event.getTournament().getId());

    }

    @Transactional
    public Tournament renameCompetitor(long eventId,
                                       long resultId,
                                       String newName) {
        Result result = em.find(Result.class, resultId);
        if (result.getEvent().getId() == eventId) {
            result.setName(newName);
            em.persist(result);
            Event event = em.find(Event.class, eventId);
            return getTournament(event.getTournament().getId());
        }
        throw new NotFoundException(
            String.format(
                "Bad Event Result ID Pair [%d,%d]",
                eventId,
                resultId));
    }

    @Transactional
    public Tournament switchSchool(long eventId,
                                   long resultId,
                                   long newSchoolId) {
        Result result = em.find(Result.class, resultId);
        if (result.getEvent().getId() == eventId) {
            School school = em.find(School.class, newSchoolId);
            result.changeSchool(school);
            Event event = em.find(Event.class, eventId);
            return getTournament(event.getTournament().getId());
        }
        throw new NotFoundException(String.format(
            "Bad Event Result ID Pair [%d,%d]",
            eventId,
            resultId));
    }

    @Transactional
    public Tournament deleteEvent(long eventId) {
        Event event = em.find(Event.class, eventId);
        Long tournamentId = event.getTournament().getId();
        em.remove(event);
        return getTournament(tournamentId);
    }

    @Transactional
    public int updateSchoolContacts(InputStream file) {
        int i = 0;
        try {
            CSVParser parse = CSVUtils.parse(file);

            for (CSVRecord csvRecord : parse.getRecords()) {
                String schoolContact = csvRecord.get("School Contact");
                String additionalContactsString = csvRecord.get("Additional Contact");
                String[] additionalContacts = additionalContactsString.split(";");
                long id = Long.parseLong(csvRecord.get("ID"));
                School school = em.getReference(School.class, id);
                em
                    .createQuery("DELETE FROM SchoolEmail where school.id=?1")
                    .setParameter(1, id)
                    .executeUpdate();
                i++;
                em.persist(SchoolEmail.fromPrimaryEmail(school, schoolContact));
                for (String additionalContact : additionalContacts) {
                    if (!additionalContact.isBlank()) {
                        em.persist(SchoolEmail.fromSecondaryEmail(school, additionalContact));
                        i++;
                    }
                }
            }

        } catch (IOException e) {
            LOG.error("Could not update contacts", e);
        }

        return i;
    }

    @Transactional
    public Tournament abbreviateEvent(long eventId, String abbreviation) {
        Event event = em.find(Event.class, eventId);
        event.setAbbreviation(abbreviation);
        em.persist(event);
        return getTournament(event.getTournament().getId());
    }

    @Transactional
    public Tournament copyTournament(long srcTournamentId) {
        final Tournament srcTournament = this.getTournament(srcTournamentId);
        final Tournament newTournament = Tournament.copy(srcTournament);
        newTournament.setName("Copy of " + newTournament.getName());
        em.persist(newTournament);
        return newTournament;
    }
}
