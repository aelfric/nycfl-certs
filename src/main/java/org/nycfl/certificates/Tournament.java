package org.nycfl.certificates;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
public class Tournament {
    @OneToMany(mappedBy = "tournament",
               fetch = FetchType.LAZY,
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    List<Event> events = new ArrayList<>();

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    private String name;

    @OneToMany(mappedBy = "tournament",
               fetch = FetchType.LAZY,
               orphanRemoval = true,
               cascade = CascadeType.ALL)
    List<School> schools = new ArrayList<>();

    public List<Event> getEvents() {
        return events;
    }

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

    public void setEvents(List<Event> events) {
        this.events.clear();
        this.events.addAll(events);
        events.forEach(e -> e.setTournament(this));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tournament that = (Tournament) o;
        return this.id != 0 && that.getId() != 0 && this.id == that.getId();
    }

    @Override
    public int hashCode() {
        return 47;
    }

    public void addSchools(Collection<School> schools) {
        this.schools.clear();
        this.schools.addAll(schools);
        schools.forEach(s -> s.setTournament(this));
    }
}
