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


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setEvent(Event event) {
        this.event = event;
    }


    private static final transient String[] numNames = {
            "",
            "First",
            "Second",
            "Third",
            "Fourth",
            "Fifth",
            "Sixth",
            "Seventh",
            "Eighth",
            "Ninth",
            "Tenth",
            "Eleventh",
            "Twelfth",
            "Thirteenth",
            "Fourteenth",
            "Fifteenth",
            "Sixteenth",
            "Seventeenth",
            "Eighteenth",
            "Nineteenth"
    };

    public String getPlaceString(){
        if(place<this.event.getPlacementCutoff()){
            return numNames[place] + " Place";
        } else if (place < this.event.getCertificateCutoff()){
            return "Finalist";
        } else {
            return "";
        }
    }

    public String getCertColor(){
        if(place==1){
            return "gold";
        } if (place==2){
            return "silver";
        } if (place == 3){
            return "bronze";
        } if(place<this.event.getPlacementCutoff()){
            return "red";
        } else {
            return "black";
        }
    }
}
