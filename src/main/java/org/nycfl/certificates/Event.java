package org.nycfl.certificates;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
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

    @ManyToOne
    @JsonbTransient
    private Tournament tournament;

    private int placementCutoff;
    private int certificateCutoff;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public void addResults(List<Result> results) {
        this.results.clear();
        this.results.addAll(results);
        results.forEach(r->r.setEvent(this));
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

    public List<Result> getCertificateResults() {
        return results
                .stream()
                .filter(r->r.place < certificateCutoff)
                .collect(Collectors.toList());
    }
}
