package org.nycfl.certificates;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ApplicationScoped
public class TournamentService {
    @Inject
    EntityManager em;

    @Transactional
    public Tournament createTournament(Tournament tournament) {
        em.persist(tournament);
        return tournament;
    }

    public List<Tournament> all() {
        List<Tournament> tournaments = em.createQuery(
                "SELECT DISTINCT t FROM Tournament t LEFT JOIN FETCH t.events e",
                Tournament.class).getResultList();
        if (!tournaments.isEmpty()){
            em.createQuery(
                    "SELECT DISTINCT e FROM Event e LEFT JOIN " +
                            "FETCH e.results WHERE e.tournament in ?1",
                    Event.class).setParameter(1, tournaments).getResultList();
        }
        return tournaments;
    }

    @Transactional
    public Tournament addEvents(EventList eventList) {
        Tournament tournament = em.createQuery(
                "SELECT t FROM Tournament t LEFT JOIN FETCH t.events WHERE t.id=?1",
                Tournament.class).setParameter(1, eventList.tournamentId)
                .getSingleResult();
        tournament.setEvents(eventList.getEvents());
        em.persist(tournament);
        return tournament;
    }

    @Transactional
    public Tournament addResults(long eventId, List<Result> results) {
        Collections.reverse(results);
        Event event = em.createQuery(
                "SELECT e FROM Event e LEFT JOIN FETCH e.tournament LEFT JOIN FETCH e.results WHERE e.id=?1",
                Event.class)
                .setParameter(1, eventId)
                .getSingleResult();
        event.addResults(results);
        em.persist(event);
        return getTournament(event.getTournament().getId());
    }

    public Tournament getTournament(Long tournamentId) {
        Tournament tournament = em.createQuery(
                "SELECT DISTINCT t FROM Tournament t LEFT JOIN FETCH t.events e " +
                        "WHERE t.id=?1",
                Tournament.class).setParameter(1, tournamentId)
                .getSingleResult();
        em.createQuery(
                "SELECT DISTINCT e FROM Event e LEFT JOIN " +
                        "FETCH e.results WHERE e.tournament=?1",
                Event.class).setParameter(1, tournament).getResultList();
        return tournament;
    }

    @Transactional
    public void addSchools(Collection<School> schools, long tournamentId) {
        Tournament tournament = em.find(Tournament.class, tournamentId);
        tournament.addSchools(schools
                .stream()
                .filter(school -> school.getTournament()==null)
                .collect(
                Collectors.toList()));
        em.persist(tournament);
    }

    public List<School> getSchools(long tournamentId) {
        return em.createQuery("SELECT s FROM School s WHERE s.tournament" +
                ".id=?1", School.class).setParameter(1, tournamentId)
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

    public List<MedalCount> getMedalCount(long tournamentId) {
        return em.createQuery("SELECT new org.nycfl.certificates.MedalCount(r" +
                ".school.name, count(r)) " +
            "FROM Event e " +
            "LEFT JOIN e.results r " +
            "WHERE e.tournament.id = ?1 " +
            "AND r.place < e.medalCutoff " +
            "GROUP BY r.school " +
            "ORDER BY r.school.name", MedalCount.class)
            .setParameter(1, tournamentId)
            .getResultList();
    }

    @Transactional
    public void updateSchool(School school, long tournamentId) {
        if(school.getId()==0) {
            school.setTournament(em.find(Tournament.class, tournamentId));
            em.persist(school);
        } else {
            em.merge(school);
        }
    }

    public AggregateSweeps getSweeps() {
        return new AggregateSweeps(em.createQuery("SELECT new org.nycfl.certificates" +
                ".SweepsResult" +
                "(s.name, coalesce(s.sweepsPoints, 0),  t.name, t.id) " +
                "FROM School s " +
                "LEFT JOIN s.tournament t " +
                "ORDER BY s.name", SweepsResult.class)
                .getResultList());
    }
    public List<SweepsResult> getSweeps(long tournamentId) {
        return em.createQuery("SELECT new org.nycfl.certificates" +
                ".SweepsResult" +
                "(s.name, coalesce(s.sweepsPoints, 0),  t.name, t.id) " +
                "FROM School s " +
                "LEFT JOIN s.tournament t " +
                "WHERE t.id = ?1" +
                "ORDER BY s.name", SweepsResult.class)
                .setParameter(1, tournamentId)
                .getResultList();
    }
}
