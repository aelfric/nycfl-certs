package org.nycfl.certificates;

import javax.json.bind.annotation.JsonbDateFormat;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Entity
public class Tournament {
    @OneToMany(mappedBy = "tournament",
               fetch = FetchType.LAZY,
               cascade = CascadeType.ALL,
               orphanRemoval = true)
            @OrderBy("name asc")
    List<Event> events = new ArrayList<>();

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    private String name;

    private String host;
    private String logoUrl;
    private String certificateHeadline;
    private String signature;

    @JsonbDateFormat(value = "yyyy-MM-dd")
    private LocalDate tournamentDate;

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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public LocalDate getDate() {
        return tournamentDate;
    }

    public void setDate(LocalDate tournamentDate) {
        this.tournamentDate = tournamentDate;
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
        this.schools.addAll(schools);
        schools.forEach(s -> s.setTournament(this));
    }

    public String getLongDate(){
        return tournamentDate.format(DateTimeFormatter.ofPattern("MMMM dd, yyyy"));
    }
    public String getShortDate(){
        return tournamentDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy"));
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getCertificateHeadline() {
        return certificateHeadline;
    }

    public void setCertificateHeadline(String certificateHeadline) {
        this.certificateHeadline = certificateHeadline;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public void merge(Tournament updatedTournament) {
        this.host = updatedTournament.host;
        this.tournamentDate = updatedTournament.tournamentDate;
        this.name = updatedTournament.name;
        this.logoUrl = updatedTournament.logoUrl;
        this.signature = updatedTournament.signature;
        this.certificateHeadline = updatedTournament.certificateHeadline;
    }
}
