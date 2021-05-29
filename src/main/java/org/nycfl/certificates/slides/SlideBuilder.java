package org.nycfl.certificates.slides;

import io.quarkus.qute.Template;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.nycfl.certificates.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@ApplicationScoped
public class SlideBuilder {

  @Inject
  Template slide;

  @ConfigProperty(name="app.data.path")
  String dataPath;

  public String buildSlidesPreview(Tournament tournament) {
      return "<html><body>" +
          String.join("", buildSlides(tournament).values()) +
          "</body></html>";
  }

  public String buildSlidesFile(Tournament tournament){
    Map<String, String> stringStringMap = buildSlides(tournament);

    try (
      FileOutputStream fileOutputStream = new FileOutputStream(getOutputFile(tournament));
      ZipOutputStream zipOutputStream = new ZipOutputStream(fileOutputStream);
      ){
      for (Map.Entry<String, String> memoryFile : stringStringMap.entrySet()) {
        ZipEntry zipEntry = new ZipEntry(memoryFile.getKey() + ".svg");
        zipOutputStream.putNextEntry(zipEntry);
        zipOutputStream.write(memoryFile.getValue().getBytes());
        zipOutputStream.closeEntry();
      }
    } catch (IOException ioException){
      throw new BadRequestException("Could not create file");
    }
    return "\"OK\"";
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
                          event.getName(),
                          round.getKey().name(),
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
