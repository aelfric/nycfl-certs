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

    @Enumerated(EnumType.STRING)
    EliminationRound eliminationRound = EliminationRound.FINALIST;


    public Long getId() {
        return id;
    }

    public void setEvent(Event event) {
        this.event = event;
    }


    public String getPlaceString(){
        return this.event.getEventType().formatPlacementString(this);
    }

    public String getCertColor(){
        return this.event.getEventType().getCertificateColor(this);
    }
}
