package org.nycfl.certificates;

import java.util.Arrays;
import java.util.List;

public record EventList(String events, long tournamentId) {
    public List<Event> getEvents() {
        return Arrays
            .stream(events.split("\n"))
            .sorted()
            .map(Event::fromName)
            .toList();
    }
}
