package org.nycfl.certificates.slides;

import io.quarkus.qute.Template;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.nycfl.certificates.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@ApplicationScoped
public class SlideBuilder {

  @Inject
  Template slide;

  @Inject
  SlideWriter slideWriter;

  @ConfigProperty(name="app.data.path")
  String dataPath;

  public String buildSlidesPreview(Tournament tournament) {
      return "<html><body>" +
          String.join("", buildSlides(tournament).values()) +
          "</body></html>";
  }

  public String buildSlidesFile(Tournament tournament){
    return slideWriter.writeSlides(
      tournament,
      buildSlides(tournament),
      "slides");
  }
  private File getOutputFile(Tournament tournament) throws IOException {
    Files.createDirectories(Paths.get(dataPath));
    return Paths.get(dataPath).resolve(
      String.format("%d_slides_%d.zip", tournament.getId(),
        System.currentTimeMillis())).toFile();
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
                      .filter(r->r.getPlace() < event.getCertificateCutoff())
                      .collect(
                          Collectors.groupingBy(it -> counter.getAndIncrement() / 9)
                      )
                      .values();
                  int i = 0;
                  for (List<Result> dividedResult : dividedResults) {
                      slides.put(String.format("%s_%s_%d",
                        event.getEventType().name(),
                        event.getName(),
                        i++),
                          slide
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
