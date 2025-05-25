package org.nycfl.certificates.slides;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateExtension;
import org.nycfl.certificates.*;
import org.nycfl.certificates.results.Result;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ApplicationScoped
@TemplateExtension
public class PostingsBuilder implements BaseAnimatedSlideBuilder {

  @Inject
  Template postings;

  @Inject
  Template posting;

  @Override
  public Map<String, String> buildSlides(Tournament tournament) {
    Map<String, String> slides = new LinkedHashMap<>();
    for (Event event : tournament.getEvents()) {
          if (event.getEventType() != EventType.DEBATE_SPEAKS) {
            final Optional<EliminationRound> maybeBreakLevel = event
                .getResults()
                .stream()
                .map(Result::getEliminationRound)
                .min(Comparator.comparingInt(Enum::ordinal));

            if(maybeBreakLevel.isEmpty()) continue;

            final EliminationRound breakLevel = maybeBreakLevel.get();

            List<Result> highestElimResults =
                  event
                      .getResults()
                      .stream()
                      .filter(r -> shouldPost(breakLevel, r))
                      .sorted(Comparator.comparing(Result::getCode))
                      .toList();

            if(!highestElimResults.isEmpty()) {
                final AtomicInteger counter = new AtomicInteger();
                final AtomicInteger slideCounter = new AtomicInteger();
//              final int i = event.getEventType() == EventType.SPEECH ? 30 : 10
              final int i = 30;
              final Map<EliminationRound, List<Result>> groupedByRound = highestElimResults
                  .stream()
                  .collect(Collectors.groupingBy(Result::getEliminationRound));

              final List<EliminationRound> rounds = groupedByRound.keySet()
                  .stream()
                  .sorted(Comparator.comparing(EliminationRound::ordinal).reversed())
                  .toList();
              for (EliminationRound round : rounds) {
                Collection<List<Result>> subSlides = groupedByRound.get(round)
                    .stream()
                    .collect(
                        Collectors.groupingBy(it -> counter.getAndIncrement() / i)
                    )
                    .values();
                counter.set(0);
                for (List<Result> subSlide : subSlides) {
                  slides.put(
                      slideName(event, slideCounter),
                      renderSlide(event, round, subSlide)
                  );
                }
              }
            }
          }
      }
    return slides;
  }

  private static boolean shouldPost(EliminationRound breakLevel, Result r) {
    if(breakLevel == r.getEliminationRound()){
      return true;
    } else {
      return breakLevel == EliminationRound.DOUBLE_OCTOFINALIST && r.getEliminationRound() == EliminationRound.PLAY_IN_BEFORE;
    }

  }

  private String slideName(Event event, AtomicInteger slideCounter) {
    return String.format(
        "%s_%s_%d",
        event.getEventType().name(),
        event.getName(),
        slideCounter.getAndIncrement());
  }

  private String renderSlide(Event event,
                               EliminationRound breakLevel,
                               List<Result> highestElimResults) {
      return posting
          .data("roundType", breakLevel.label)
          .data("event", event)
          .data("round", breakLevel.label)
          .data("results", highestElimResults)
          .render();
    }

  public String buildSlidesPreview(Tournament tournament) {
    return this.buildSlidesPreview(tournament, postings);
  }
}
