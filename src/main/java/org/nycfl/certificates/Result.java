package org.nycfl.certificates;

import javax.json.bind.annotation.JsonbTransient;
import javax.persistence.*;

@Entity
public class Result {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    long id;
    int count;
    int place;
    String code;

    public void setName(String name) {
        this.name = name;
    }

    String name;

    public int getCount() {
        return count;
    }

    public int getPlace() {
        return place;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public Event getEvent() {
        return event;
    }

    public School getSchool() {
        return school;
    }

    @JsonbTransient
    @ManyToOne(optional = false)
    Event event;
    @ManyToOne(optional = false)
    School school;

    @Column(name = "NUM_WINS")
    Integer numWins = 0;

    @Enumerated(EnumType.STRING)
    EliminationRound eliminationRound = EliminationRound.FINALIST;


    public Long getId() {
        return id;
    }

    public void setEvent(Event event) {
        this.event = event;
    }


    public String getPlaceString(){
        return this.event.formatResult(this);
    }

    public String getCertColor(){
        return this.event.getCertificateColor(this);
    }

    public Integer getNumWins() {
        return numWins;
    }

    public void setNumWins(int numWins) {
        this.numWins = numWins;
    }

    public EliminationRound getEliminationRound() {
        return eliminationRound;
    }

    public String getHtmlName(){
        return name == null ? "" : name.replace("&", "&amp;");
    }

    public void setSchool(School school) {
        this.school.getResults().remove(this);
        this.school = school;
        school.getResults().add(this);
    }

    @Override
    public boolean equals(Object that){
        if(this == that) return true;
        if(that instanceof Result) {
            Result other = (Result) that;
            return this.id == other.id && this.id != 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 7;
    }
}
