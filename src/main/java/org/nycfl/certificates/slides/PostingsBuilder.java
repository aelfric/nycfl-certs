package org.nycfl.certificates.slides;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateExtension;
import org.nycfl.certificates.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
                      .filter(r -> breakLevel == r.getEliminationRound())
                      .sorted(Comparator.comparing(Result::getCode))
                      .collect(Collectors.toList());

            if(!highestElimResults.isEmpty()) {
                final AtomicInteger counter = new AtomicInteger();
                final AtomicInteger slideCounter = new AtomicInteger();
              final int i = event.getEventType() == EventType.SPEECH || event.getEventType() == EventType.CONGRESS ? 28 : 8;
              Collection<List<Result>> subSlides = highestElimResults
                  .stream()
                  .collect(
                    Collectors
                      .groupingBy(it -> counter.getAndIncrement() / i)
                  )
                  .values();
                for (List<Result> subSlide : subSlides) {
                  slides.put(
                      slideName(event, slideCounter),
                      renderSlide(event, breakLevel, subSlide)
                  );
                }
            }
          }
      }
    return slides;
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
