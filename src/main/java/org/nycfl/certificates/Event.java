package org.nycfl.certificates;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    private String name;

    public List<Result> getResults() {
        return results;
    }

    @OneToMany(mappedBy = "event",
               fetch = FetchType.LAZY,
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    @OrderBy("place desc ")
    private List<Result> results = new ArrayList<>();

    @ManyToOne(optional = false)
    @JsonbTransient
    private Tournament tournament;

    private int placementCutoff;
    private int certificateCutoff;
    private int medalCutoff;

    @Enumerated(EnumType.STRING)
    private EventType eventType = EventType.SPEECH;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static Event fromName(String name) {
        Event event = new Event();
        event.setName(name);
        return event;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public Tournament getTournament() {
        return this.tournament;
    }

    public void addResults(List<Result> newResults) {
        Map<String, Result> resultsByCode = this.results.stream()
                .collect(Collectors.toMap(
                        r -> r.code,
                        Function.identity()));
        for (Result newResult : newResults) {
            if(resultsByCode.containsKey(newResult.code)){
                Result updatedResult = resultsByCode.get(newResult.code);
                updatedResult.eliminationRound = newResult.eliminationRound;
                updatedResult.place = newResult.place;
                this.results.replaceAll(oldResult -> {
                            if (oldResult.code.equals(newResult.code)) {
                                return newResult;
                            } else {
                                return oldResult;
                            }
                        }
                );
            } else {
                this.results.add(newResult);
            }
        }
        newResults.forEach(r->r.setEvent(this));
    }

    public int getPlacementCutoff() {
        return placementCutoff;
    }

    public void setPlacementCutoff(int placementCutoff) {
        this.placementCutoff = placementCutoff;
    }

    public int getCertificateCutoff() {
        return certificateCutoff;
    }

    public void setCertificateCutoff(int certificateCutoff) {
        this.certificateCutoff = certificateCutoff;
    }

    public int getMedalCutoff() {
        return medalCutoff;
    }

    public void setMedalCutoff(int medalCutoff) {
        this.medalCutoff = medalCutoff;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    void parseResults(EliminationRound eliminationRound,
                      InputStream csvInputStream,
                      Map<String, School> schoolsMap) {
        List<Result> results = eventType.parseResults(
                schoolsMap,
                eliminationRound,
                csvInputStream
        );
        Collections.reverse(results);
        addResults(results);
    }
}
