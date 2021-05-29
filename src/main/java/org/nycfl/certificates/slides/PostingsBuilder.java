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
public class PostingsBuilder {

  private static int perColumn = 8;
  @Inject
  Template posting;

  @Inject
  SlideWriter slideWriter;

  @SuppressWarnings("unused")
  public static double getXOffset(int index){
    final int column = (index - 1) / perColumn;
    return (float) 237.35547 + column * 65;
  }

  @SuppressWarnings("unused")
  public static double getPostingFontSize(String newText, int textBaseSize){
    int newLength = newText.length();
    double charsPerLine = 35.0;
    double newEmSize = charsPerLine / newLength;

    // Applying ems directly was causing some weirdness, converting ems to pixels got rid of the weirdyness
    if(newEmSize < 1){
      return newEmSize * textBaseSize;
    } else {
      // It fits, leave it alone
      return textBaseSize;
    }
  }

  @SuppressWarnings("unused")
  public static boolean getIsStart(int count){
    perColumn = 8;
    return (count - 1) % perColumn == 0 ;
  }

  public String buildSlidesPreview(Tournament tournament) {
    Map<String, String> stringStringMap = buildSlides(tournament);
    return "<html><body>" +
          String.join("", stringStringMap.values()) +
          "</body></html>";
  }

  public String buildSlidesFile(Tournament tournament){
    return slideWriter.writeSlides(
      tournament,
      buildSlides(tournament),
      "postings");
  }

  Map<String, String> buildSlides(Tournament tournament) {
    Map<String, String> slides = new LinkedHashMap<>();
    for (Event event : tournament.getEvents()) {
          if (event.getEventType() != EventType.DEBATE_SPEAKS) {
            final Optional<EliminationRound> breakLevel = event
                .getResults()
                .stream()
                .map(Result::getEliminationRound)
                .min(Comparator.comparingInt(Enum::ordinal));
            List<Result> highestElimResults =
                  event
                      .getResults()
                      .stream()
                      .filter(r -> breakLevel.isPresent() && breakLevel.get() == r.getEliminationRound())
                      .sorted(Comparator.comparing(Result::getCode))
                      .collect(Collectors.toList());

            int codeLength =
                highestElimResults.stream().map(Result::getCode)
                    .mapToInt(String::length).max().orElse(4);

            if(!highestElimResults.isEmpty()) {
                final AtomicInteger counter = new AtomicInteger();
                final AtomicInteger slideCounter = new AtomicInteger();
                Collection<List<Result>> subSlides = highestElimResults
                  .stream()
                  .collect(
                    Collectors
                      .groupingBy(it -> counter.getAndIncrement() / (codeLength <= 4 ? perColumn * 4 : perColumn))
                  )
                  .values();
                for (List<Result> subSlide : subSlides) {
                  slides.put(
                    String.format("%s_%s_%d",
                    event.getEventType().name(),
                    event.getName(),
                    slideCounter.getAndIncrement()),
                    renderSlide(
                      event,
                      breakLevel,
                      subSlide
                    )
                  );
                }
            }
          }
      }
    return slides;
  }

    private String renderSlide(Event event,
                               Optional<EliminationRound> breakLevel,
                               List<Result> highestElimResults) {
        return posting
            .data("results", highestElimResults)
            .data("event", event)
            .data("round",
                breakLevel
                    .map(EliminationRound::getLabel)
                    .orElse(""))
            .render();
    }

}
