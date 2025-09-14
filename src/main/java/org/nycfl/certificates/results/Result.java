package org.nycfl.certificates.results;

import org.nycfl.certificates.EliminationRound;
import org.nycfl.certificates.Event;
import org.nycfl.certificates.School;

import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

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

    public void changeSchool(School school) {
        this.school.getResults().remove(this);
        this.school = school;
        school.getResults().add(this);
    }

    public void setSchool(School school){
        this.school = school;
        school.getResults().add(this);
    }

    @Override
    public boolean equals(Object that){
        if(this == that) return true;
        if(that instanceof Result other) {
            return this.id == other.id && this.id != 0;
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return 7;
    }

    public void setEliminationRound(EliminationRound eliminationRound) {
        this.eliminationRound = eliminationRound;
    }

    public void setPlace(int place) {
        this.place = place;
    }

    public void setCode(String code) {
        this.code = code;
    }

    @Override
    public String toString() {
        return "Result{" +
               "code='" + code + '\'' +
               ", name='" + name + '\'' +
               '}';
    }
}
