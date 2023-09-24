package org.nycfl.certificates;

import org.nycfl.certificates.results.Result;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import java.io.InputStream;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Entity
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE)
    private long id;

    private String name;

    private String abbreviation;

    public List<Result> getResults() {
        return results;
    }

    @OneToMany(mappedBy = "event",
               fetch = FetchType.LAZY,
               cascade = CascadeType.ALL,
               orphanRemoval = true)
    @OrderBy("place desc ")
    private final List<Result> results = new ArrayList<>();

    @ManyToOne(optional = false)
    @JsonbTransient
    private Tournament tournament;

    private int placementCutoff;
    private int certificateCutoff;
    private int medalCutoff;
    @Column(name = "HALF_QUALS", columnDefinition = "int DEFAULT 0 NOT NULL")
    private int halfQuals;

    @Column(name = "NUM_ROUNDS")
    private Integer numRounds = 0;

    @Enumerated(EnumType.STRING)
    private EventType eventType = EventType.SPEECH;

    @Enumerated(EnumType.STRING)
    private CertificateType certificateType = CertificateType.PLACEMENT;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public static Event fromName(String name) {
        Event event = new Event();
        event.setName(name);
        return event;
    }

    public void setTournament(Tournament tournament) {
        this.tournament = tournament;
    }

    public Tournament getTournament() {
        return this.tournament;
    }

    @JsonbProperty("latestResult")
    public Optional<String> getLatestResult(){
        return results.stream().map(Result::getEliminationRound).min(
          Comparator.comparingInt(EliminationRound::ordinal)
        ).map(EliminationRound::getLabel);
    }

    public void addResults(List<Result> newResults) {
        Map<String, Result> resultsByCode = this.results.stream()
                .collect(Collectors.toMap(
                    Result::getCode,
                        Function.identity()));
        for (Result newResult : newResults) {
            if(resultsByCode.containsKey(newResult.getCode())){
                Result updatedResult = resultsByCode.get(newResult.getCode());
                updatedResult.setEliminationRound(newResult.getEliminationRound());
                updatedResult.setPlace(newResult.getPlace());
                this.results.replaceAll(oldResult -> {
                            if (oldResult.getCode().equals(newResult.getCode())) {
                                return newResult;
                            } else {
                                return oldResult;
                            }
                        }
                );
            } else {
                this.results.add(newResult);
            }
        }
        newResults.forEach(r->r.setEvent(this));
    }

    public int getPlacementCutoff() {
        return placementCutoff;
    }

    public void setPlacementCutoff(int placementCutoff) {
        this.placementCutoff = placementCutoff;
    }

    public int getCertificateCutoff() {
        return certificateCutoff;
    }

    public void setCertificateCutoff(int certificateCutoff) {
        this.certificateCutoff = certificateCutoff;
    }

    public int getMedalCutoff() {
        return medalCutoff;
    }

    public void setMedalCutoff(int medalCutoff) {
        this.medalCutoff = medalCutoff;
    }

    public EventType getEventType() {
        if(eventType==null) return EventType.SPEECH;
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public CertificateType getCertificateType() {
        if(certificateType==null) return CertificateType.PLACEMENT;
        return certificateType;
    }

    public void setCertificateType(CertificateType certificateType) {
        this.certificateType = certificateType;
    }

    public Integer getNumRounds() {
        return numRounds;
    }

    public void setNumRounds(int numRounds) {
        this.numRounds = numRounds;
    }

    public int getHalfQuals() {
        return halfQuals;
    }

    public void setHalfQuals(int halfQuals) {
        this.halfQuals = halfQuals;
    }

    void parseResults(EliminationRound eliminationRound,
                      InputStream csvInputStream,
                      Map<String, School> schoolsMap) {
        List<Result> parsedResults = eventType.parseResults(
                schoolsMap,
                eliminationRound,
                csvInputStream
        );
        Collections.reverse(parsedResults);
        addResults(parsedResults);
    }

    Function<School, String> getSchoolMappingFunction() {
        return eventType.schoolMapper();
    }

    public void clearResults() {
        results.clear();
    }

    @SuppressWarnings("java:S3358")
    public String formatResult(Result result) {
        return switch (getCertificateType()) {
            case PLACEMENT, DEBATE_SPEAKER, CONGRESS_PO -> getEventType()
                .formatPlacementString(result);
            case DEBATE_RECORD -> {
                int wins = result.getNumWins() != null ? result.getNumWins() : 0;
                int total = numRounds != null ? numRounds : 0;
                yield String.format("%d-%d",
                    wins,
                    total - wins);
            }
            case QUALIFIER -> result.getPlace() < getPlacementCutoff() ?
                "Qualifier" : result.getPlace() < getCertificateCutoff() ?
                "Alternate" : "";
        };
    }

    public String getCertificateColor(Result result) {
        return switch (getCertificateType()) {
            case PLACEMENT, CONGRESS_PO -> getEventType().getCertificateColor(result);
            case DEBATE_SPEAKER -> "black";
            case DEBATE_RECORD -> {
                int wins = result.getNumWins() != null ? result.getNumWins() : 0;
                int total = numRounds != null ? numRounds : 0;
                if(wins == total){
                    yield "gold";
                } else if (wins == total - 1){
                    yield "silver";
                } else {
                    yield "red";
                }
            }
            case QUALIFIER -> result.getPlace() < placementCutoff ? "gold" :
                "silver";
        };
    }

    long countCertificates() {
        return getResults()
            .stream()
            .filter(result -> result.getPlace() < getCertificateCutoff())
            .count();
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }
}
