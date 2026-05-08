package org.nycfl.certificates.lastround;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.persistence.*;
import org.nycfl.certificates.Event;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
public class EventMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @ManyToOne
    LastRoundImport report;

    @JsonbProperty("eventRaw")
    public String getEventRaw() {
        return eventRaw;
    }

    @JsonbProperty("event")
    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    @JsonbProperty("rounds")
    public List<RoundMapping> getRoundMappings() {
        return roundMappings;
    }

    public void setRoundMappings(List<RoundMapping> roundMappings) {
        this.roundMappings = roundMappings;
    }

    String eventRaw;

    @OneToOne
    Event event;

    @OneToMany(mappedBy = "eventMapping", cascade = CascadeType.PERSIST)
    List<RoundMapping> roundMappings = new ArrayList<>();

    public void setReport(LastRoundImport lastRoundImport) {
        this.report = lastRoundImport;
    }

    public EventMapping setEventRaw(String eventRaw) {
        this.eventRaw = eventRaw;
        return this;
    }

    public EventMapping addRoundMappings(Collection<RoundMapping> list) {
        list.forEach(m -> roundMappings.add(m.setEventMapping(this)));
        return this;
    }
}
