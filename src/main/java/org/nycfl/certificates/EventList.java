package org.nycfl.certificates;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import java.util.Arrays;
import java.util.List;

public class EventList {
    public final long tournamentId;
    public final String events;

    @JsonbCreator
    public EventList(
            @JsonbProperty("events") String events,
            @JsonbProperty("tournamentId") long id) {
        this.events = events;
        this.tournamentId = id;
    }

    public List<Event> getEvents() {
        return Arrays
                .stream(events.split("\n"))
                .sorted()
                .map(Event::fromName)
                .toList();
    }
}
