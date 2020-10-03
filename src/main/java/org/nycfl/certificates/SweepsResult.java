package org.nycfl.certificates;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import java.util.Objects;

public class SweepsResult {
  public final String school;
  public final int points;
  public final String tournament;
  public final long tournamentId;

  @JsonbCreator
  public SweepsResult(@JsonbProperty("school") String school,
                      @JsonbProperty("points") int points,
                      @JsonbProperty("tournament") String tournament,
                      @JsonbProperty("tournamentId") long tournamentId) {
    this.school = school;
    this.points = points;
    this.tournament = tournament;
    this.tournamentId = tournamentId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SweepsResult that = (SweepsResult) o;
    return points == that.points &&
            Objects.equals(school, that.school) &&
            Objects.equals(tournament, that.tournament);
  }

  @Override
  public int hashCode() {
    return Objects.hash(school, points, tournament);
  }

  @Override
  public String toString() {
    return "SweepsResult{" +
            "school='" + school + '\'' +
            ", count=" + points +
            ", tournament='" + tournament + '\'' +
            '}';
  }
}
