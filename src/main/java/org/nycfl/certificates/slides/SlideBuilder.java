package org.nycfl.certificates.slides;

import io.quarkus.qute.Template;
import org.nycfl.certificates.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ApplicationScoped
public class SlideBuilder {

  @Inject
  Template slide2;

  @Inject
  Template slides;

  @Inject
  SlideWriter slideWriter;

  public String buildSlidesPreview(Tournament tournament) {
    String slideBackgroundUrl = tournament.getSlideBackgroundUrl();
    return slides.render(
      Map.of(
        "slides",
        buildSlides(tournament)
          .entrySet()
          .stream()
          .sorted(Map.Entry.comparingByKey())
          .map(Map.Entry::getValue),
        "image", slideBackgroundUrl == null ? "" : slideBackgroundUrl,
        "accentColor", tournament.getSlideAccentColor(),
        "secondaryAccentColor", tournament.getSlideSecondaryAccentColor(),
        "primaryColor", tournament.getSlidePrimaryColor()
      ));
  }

  public String buildSlidesFile(Tournament tournament) {
    return slideWriter.writeSlides(
      tournament,
      buildSlides(tournament),
      "slides");
  }

  Map<String, String> buildSlides(Tournament tournament) {
    Map<String, String> slides = new LinkedHashMap<>();
    for (Event event : tournament.getEvents()) {
      if (event.getEventType() != EventType.DEBATE_SPEAKS) {
        Map<EliminationRound, List<Result>> collect =
          event
            .getResults()
            .stream()
            .sorted(Comparator.comparing(Result::getName))
            .collect(Collectors.groupingBy(Result::getEliminationRound));
        for (Map.Entry<EliminationRound, List<Result>> round : collect.entrySet()) {
          final AtomicInteger counter = new AtomicInteger();

          Collection<List<Result>> dividedResults = round
            .getValue()
            .stream()
            .filter(r -> r.getPlace() < event.getCertificateCutoff())
            .collect(
              Collectors.groupingBy(it -> counter.getAndIncrement() / 9)
            )
            .values();
          int i = 0;

          int roundIndex =
            EliminationRound.values().length - round.getKey().ordinal();
          for (List<Result> dividedResult : dividedResults) {
            slides.put(String.format("%s_%s_%d_%d",
                event.getEventType().name(),
                event.getName(),
                roundIndex,
                i++),
              slide2
                .data("roundType", round.getKey().label)
                .data("slideBackground", tournament.getSlideBackgroundUrl())
                .data("event", event)
                .data("round", round.getKey().label)
                .data("results", dividedResult)
                .render());
          }
        }
      }
    }
    return slides;
  }
}
