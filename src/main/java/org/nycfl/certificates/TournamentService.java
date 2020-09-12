package org.nycfl.certificates;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.Collection;
import java.util.List;

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
        tournament.addSchools(schools);
        em.persist(tournament);
    }

    public List<School> getSchools(long tournamentId) {
        return em.createQuery("SELECT s FROM School s WHERE s.tournament" +
                ".id=?1", School.class).setParameter(1, tournamentId)
                .getResultList();
    }
}
