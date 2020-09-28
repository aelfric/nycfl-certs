package org.nycfl.certificates;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.*;

@Entity
public class School {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;
    private String name;
    private String displayName;
    @ManyToOne
    @JsonbTransient
    private Tournament tournament;
    private int sweepsPoints = 0;

    public static School fromName(String name) {
        School school = new School();
        school.name = name;
        school.displayName = name;
        return school;
    }

    public Long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Tournament getTournament() {
        return tournament;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public Integer getSweepsPoints() {
        return sweepsPoints;
    }

    public void setSweepsPoints(int sweepsPoints) {
        this.sweepsPoints = sweepsPoints;
    }
}
