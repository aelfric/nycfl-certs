package org.nycfl.certificates;

import javax.json.bind.annotation.JsonbCreator;
import javax.json.bind.annotation.JsonbProperty;
import java.util.Objects;

public class MedalCount {
  public final String school;
  public final long count;

  @JsonbCreator
  public MedalCount(@JsonbProperty("school") String school,
                    @JsonbProperty("count") long count) {
    this.school = school;
    this.count = count;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    MedalCount that = (MedalCount) o;
    return count == that.count &&
        school.equals(that.school);
  }

  @Override
  public int hashCode() {
    return Objects.hash(school, count);
  }

  @Override
  public String toString() {
    return "MedalCount{" +
        "school='" + school + '\'' +
        ", count=" + count +
        '}';
  }
}
