package org.nycfl.certificates;

import jakarta.json.bind.annotation.JsonbProperty;
import jakarta.json.bind.annotation.JsonbTransient;
import jakarta.persistence.*;
import java.util.Objects;

@Entity
public class SchoolEmail {
  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE)
  long id;

  @JsonbProperty("email")
  public String getEmail() {
    return email;
  }

  @JsonbProperty("isPrimary")
  public boolean isPrimary() {
    return isPrimary;
  }

  String email;

  boolean isPrimary;

  @JsonbTransient
  @ManyToOne(optional = false)
  School school;

  protected SchoolEmail(){

  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SchoolEmail that = (SchoolEmail) o;
    return isPrimary == that.isPrimary && email.equals(that.email);
  }

  @Override
  public int hashCode() {
    return Objects.hash(email, isPrimary);
  }

  private SchoolEmail(School school, String email, boolean isPrimary){
    this.email = email;
    this.isPrimary = isPrimary;
    this.school = school;
  }

  public static SchoolEmail fromPrimaryEmail(School school, String email){
    return new SchoolEmail(school, email, true);
  }

  public static SchoolEmail fromSecondaryEmail(School school, String email){
    return new SchoolEmail(school, email, false);
  }

  @Override
  public String toString() {
    return "SchoolEmail{" +
      "id=" + id +
      ", email='" + email + '\'' +
      ", isPrimary=" + isPrimary +
      '}';
  }
}
