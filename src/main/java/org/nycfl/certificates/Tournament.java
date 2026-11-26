package org.nycfl.certificates;

import jakarta.json.bind.annotation.JsonbDateFormat;
import jakarta.persistence.*;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.nycfl.certificates.requests.TournamentInput;

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
            @OrderBy("eventType asc, name asc")
    final List<Event> events = new ArrayList<>();

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    private String name;

    private String host;
    private String logoUrl;
    private String slideBackgroundUrl;
    private String certificateHeadline;
    private String signature;
    private String signatureTitle;
    private String line1;
    private String line2;
    private String circuitId;

    @Column(columnDefinition = "VARCHAR(32) default '#00356b'")
    private String slideAccentColor;

    @Column(columnDefinition = "VARCHAR(32) default '#4a4a4a'")
    private String slideSecondaryAccentColor;

    @Column(columnDefinition = "VARCHAR(32) default '#222222'")
    private String slidePrimaryColor;

    @Column(columnDefinition = "VARCHAR(32) default '#dddddd'")
        private String slideOverlayColor;

    @Schema(name = "date")
    @JsonbDateFormat(value = "yyyy-MM-dd")
    private LocalDate tournamentDate;

    @OneToMany(mappedBy = "tournament",
               fetch = FetchType.LAZY,
               orphanRemoval = true,
               cascade = CascadeType.ALL)
    final List<School> schools = new ArrayList<>();

    @Lob
    @Schema(name = "styleOverrides")
    private String styleOverrides2;

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

    public String getLine1() {
        return line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getCertLine1() {
        return line1 != null ? line1 : host;
    }

    public String getCertLine2() {
        return line2 != null ? line2 : name;
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

    public String getSignatureTitle() {
        return signatureTitle;
    }

    public void setSignatureTitle(String signatureTitle) {
        this.signatureTitle = signatureTitle;
    }

    public String getSlideBackgroundUrl() {
        return slideBackgroundUrl;
    }

    public void setSlideBackgroundUrl(String slideBackgroundUrl) {
        this.slideBackgroundUrl = slideBackgroundUrl;
    }

    public void merge(TournamentInput tournamentUpdateRequest) {
        this.host = tournamentUpdateRequest.host();
        this.tournamentDate = tournamentUpdateRequest.date();
        this.name = tournamentUpdateRequest.name();
        this.logoUrl = tournamentUpdateRequest.logoUrl();
        this.slideBackgroundUrl = tournamentUpdateRequest.slideBackgroundUrl();
        this.slideAccentColor = tournamentUpdateRequest.slideAccentColor();
        this.slideSecondaryAccentColor = tournamentUpdateRequest.slideSecondaryAccentColor();
        this.slidePrimaryColor = tournamentUpdateRequest.slidePrimaryColor();
        this.slideOverlayColor = tournamentUpdateRequest.slideOverlayColor();
        this.signature = tournamentUpdateRequest.signature();
        this.signatureTitle = tournamentUpdateRequest.signatureTitle();
        this.certificateHeadline = tournamentUpdateRequest.certificateHeadline();
        this.styleOverrides2 = tournamentUpdateRequest.styleOverrides();
        this.line1 = tournamentUpdateRequest.line1();
        this.line2 = tournamentUpdateRequest.line2();
    }

    public static Tournament copy(Tournament source){
        final Tournament tournament = new Tournament();
        tournament.merge(TournamentInput.from(source));
        return tournament;
    }

    public String getStyleOverrides() {
        return styleOverrides2;
    }

    public void setStyleOverrides(String styleOverrides) {
        this.styleOverrides2 = styleOverrides;
    }

    public String getSlideAccentColor() {
        return slideAccentColor;
    }

    public void setSlideAccentColor(String slideAccentColor) {
        this.slideAccentColor = slideAccentColor;
    }

    public String getSlidePrimaryColor() {
        return slidePrimaryColor;
    }

    public void setSlidePrimaryColor(String slidePrimaryColor) {
        this.slidePrimaryColor = slidePrimaryColor;
    }

    public String getSlideSecondaryAccentColor() {
        return slideSecondaryAccentColor;
    }

    public String getSlideOverlayColor(){
        return this.slideOverlayColor;
    }
    public void setSlideOverlayColor(String slideOverlayColor){
        this.slideOverlayColor = slideOverlayColor;
    }

    public void setSlideSecondaryAccentColor(String slideSecondaryAccentColor) {
        this.slideSecondaryAccentColor = slideSecondaryAccentColor;
    }

    public String getCircuitId() {
        return circuitId;
    }

    public void setCircuitId(String circuitId) {
        this.circuitId = circuitId;
    }
}
