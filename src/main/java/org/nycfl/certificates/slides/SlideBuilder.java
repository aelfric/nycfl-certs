package org.nycfl.certificates.slides;

import io.quarkus.qute.Template;
import org.nycfl.certificates.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ApplicationScoped
public class SlideBuilder implements BaseAnimatedSlideBuilder{

  @Inject
  Template slide2;

  @Inject
  Template slides;

  public String buildSlidesPreview(Tournament tournament) {
    return this.buildSlidesPreview(tournament, slides);
  }

  @Override
  public Map<String, String> buildSlides(Tournament tournament) {
    Map<String, String> slideMap = new LinkedHashMap<>();
    for (Event event : tournament.getEvents()) {
      if (event.getEventType() != EventType.DEBATE_SPEAKS) {
        Map<EliminationRound, List<Result>> collect =
          event
            .getResults()
            .stream()
            .sorted(Comparator.comparing(Result::getPlace).reversed())
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
            slideMap.put(String.format("%s_%s_%d_%d",
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
    return slideMap;
  }
}
