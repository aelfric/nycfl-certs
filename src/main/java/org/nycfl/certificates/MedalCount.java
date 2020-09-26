package org.nycfl.certificates;

public class MedalCount {
  public final String school;
  public final long count;
  public final long sweeps;

  public MedalCount(String school, long count, int sweeps) {
    this.school = school;
    this.count = count;
    this.sweeps = sweeps;
  }
}
