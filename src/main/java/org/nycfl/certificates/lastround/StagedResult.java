package org.nycfl.certificates.lastround;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;

import java.util.Objects;

@Entity
public final class StagedResult {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    @ManyToOne
    @JsonbTransient
    private LastRoundImport report;

    @JsonProperty("School")
    private String school;
    @JsonProperty("SchCode")
    private String schoolCode;
    @JsonProperty("Code")
    private String code;
    @JsonProperty("Name")
    private String name;
    @JsonProperty("Full Names")
    private String fullName;
    @JsonProperty("Entry Code")
    private String entryCode;
    @JsonProperty("Event")
    private String event;
    @JsonProperty("Category")
    private String category;
    @JsonProperty("Place")
    private String place;
    @JsonProperty("Last")
    private String last;
    @JsonProperty("Round")
    private String round;

    public StagedResult(){

    }

    @JsonCreator
    public StagedResult(
        @JsonProperty("School") String school,
        @JsonProperty("SchCode") String schoolCode,
        @JsonProperty("Code") String code,
        @JsonProperty("Name") String name,
        @JsonProperty("Full Names") String fullName,
        @JsonProperty("Entry Code") String entryCode,
        @JsonProperty("Event") String event,
        @JsonProperty("Category") String category,
        @JsonProperty("Place") String place,
        @JsonProperty("Last") String last,
        @JsonProperty("Round") String round
    ) {
        this.school = school;
        this.schoolCode = schoolCode;
        this.code = code;
        this.name = name;
        this.fullName = fullName;
        this.entryCode = entryCode;
        this.event = event;
        this.category = category;
        this.place = place;
        this.last = last;
        this.round = round;
    }

    @JsonProperty("School")
    public String school() {
        return school;
    }

    @JsonProperty("SchCode")
    public String schoolCode() {
        return schoolCode;
    }

    @JsonProperty("Code")
    public String code() {
        return code;
    }

    @JsonProperty("Name")
    public String name() {
        return name;
    }

    @JsonProperty("Full Names")
    public String fullName() {
        return fullName;
    }

    @JsonProperty("Entry Code")
    public String entryCode() {
        return entryCode;
    }

    @JsonProperty("Event")
    public String event() {
        return event;
    }

    @JsonProperty("Category")
    public String category() {
        return category;
    }

    @JsonProperty("Place")
    public String place() {
        return place;
    }

    @JsonProperty("Last")
    public String last() {
        return last;
    }

    @JsonProperty("Round")
    public String round() {
        return round;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (StagedResult) obj;
        return Objects.equals(this.school, that.school) &&
               Objects.equals(this.schoolCode, that.schoolCode) &&
               Objects.equals(this.code, that.code) &&
               Objects.equals(this.name, that.name) &&
               Objects.equals(this.fullName, that.fullName) &&
               Objects.equals(this.entryCode, that.entryCode) &&
               Objects.equals(this.event, that.event) &&
               Objects.equals(this.category, that.category) &&
               Objects.equals(this.place, that.place) &&
               Objects.equals(this.last, that.last) &&
               Objects.equals(this.round, that.round);
    }

    @Override
    public int hashCode() {
        return Objects.hash(school, schoolCode, code, name, fullName, entryCode, event, category, place, last, round);
    }

    @Override
    public String toString() {
        return "LastRound[" +
               "school=" + school + ", " +
               "schoolCode=" + schoolCode + ", " +
               "code=" + code + ", " +
               "name=" + name + ", " +
               "fullName=" + fullName + ", " +
               "entryCode=" + entryCode + ", " +
               "event=" + event + ", " +
               "category=" + category + ", " +
               "place=" + place + ", " +
               "last=" + last + ", " +
               "round=" + round + ']';
    }

    public void setReport(LastRoundImport lastRoundImport) {
        this.report = lastRoundImport;
    }
}
