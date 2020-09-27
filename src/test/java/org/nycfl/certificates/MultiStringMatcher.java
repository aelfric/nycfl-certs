package org.nycfl.certificates;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiStringMatcher extends TypeSafeMatcher<String> {
  private final Pattern pattern;
  private final int numMatches;

  public MultiStringMatcher(String regex, int n) {
    this.pattern = Pattern.compile(regex);
    this.numMatches = n;
  }

  static MultiStringMatcher containsStringNTimes(String substring, int n) {
    return new MultiStringMatcher(substring, n);
  }

  protected boolean matchesSafely(String item) {
    int count = 0;
    Matcher matcher = pattern.matcher(item);
    while (matcher.find()) {
      count++;
    }

    return count == numMatches;
  }

  public void describeTo(Description description) {
    description.appendText("a string matching the pattern '" + this.pattern + "'");
  }
}
