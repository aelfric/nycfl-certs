package org.nycfl.certificates.lastround;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import org.nycfl.certificates.Tournament;
import org.nycfl.certificates.Event;

import java.util.*;

@Entity
public class LastRoundImport {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @ManyToOne
    @JsonbTransient
    private Tournament tournament;

    @OneToMany(mappedBy = "report", cascade = CascadeType.PERSIST)
    List<StagedResult> rows = new ArrayList<>();

    @OneToMany(mappedBy = "report", cascade = CascadeType.PERSIST)
    List<EventMapping> eventMappings = new ArrayList<>();

    public void addRow(StagedResult row) {
        row.setReport(this);
        this.rows.add(row);
    }

    public void addEventMapping(EventMapping eventMapping) {
        eventMapping.setReport(this);
        this.eventMappings.add(eventMapping);
    }

    public static Builder builder(Tournament tournament) {
        return new Builder(tournament);
    }

    public LastRoundImport applyEventMapping(Map<String, Event> mapping) {
        for (EventMapping eventMapping : this.eventMappings) {
            eventMapping.setEvent(mapping.get(eventMapping.eventRaw));
        }
        return this;
    }

    public static class Builder {
        private final Tournament tournament;
        Set<String> eventsRaw = new HashSet<>();
        List<StagedResult> rows = new ArrayList<>();
        Map<String, Set<String>> roundsRaw = new HashMap<>();


        public Builder(Tournament tournament) {
            this.tournament = tournament;
        }

        public void addRow(StagedResult row) {
            this.eventsRaw.add(row.event());
            this.roundsRaw.compute(row.event(), (s, strings) -> {
                Set<String> rounds = Objects.requireNonNullElse(strings, new HashSet<>());
                rounds.add(row.last());
                return rounds;
            });
            this.rows.add(row);
        }

        public LastRoundImport build() {
            LastRoundImport anImport = new LastRoundImport();
            this.rows.forEach(anImport::addRow);
            this.eventsRaw
                .stream()
                .map(eventName ->
                    new EventMapping()
                        .setEventRaw(eventName)
                        .addRoundMappings(
                            this.roundsRaw.get(eventName).stream().map(round ->
                                new RoundMapping().setRoundRaw(round)
                            ).toList()
                        )
                )
                .forEach(anImport::addEventMapping);
            return anImport;
        }
    }
}
