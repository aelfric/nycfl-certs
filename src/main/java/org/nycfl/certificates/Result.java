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
    @JsonbTransient
    @ManyToOne
    Event event;
    @ManyToOne
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
}
