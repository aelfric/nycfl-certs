package org.nycfl.certificates.slides;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateExtension;
import org.nycfl.certificates.*;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

@ApplicationScoped
@TemplateExtension
public class PostingsBuilder {

  @Inject
  Template posting;

  public static double getXOffset(int index, int total){
    final int offset = 445 - (total / 6 + 1 ) * 60;
//    return offset + ((index - 1) / 6) * 60;
    return 444.5 / 2;
  }

  public static boolean getIsStart(int count){
    return (count - 1) % 6 == 0 ;
  }

  public String buildSlidesPreview(Tournament tournament) {
      return "<html><body>" +
          String.join("", buildSlides(tournament).values()) +
          "</body></html>";
  }

//  public byte[] buildSlidesFile(Tournament tournament){
//    Map<String, String> stringStringMap = buildSlides(tournament);
//    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
//
//    try (ZipOutputStream zipOutputStream = new ZipOutputStream(byteArrayOutputStream)){
//      for (Map.Entry<String, String> memoryFile : stringStringMap.entrySet()) {
//        ZipEntry zipEntry = new ZipEntry(memoryFile.getKey() + ".svg");
//        zipOutputStream.putNextEntry(zipEntry);
//        zipOutputStream.write(memoryFile.getValue().getBytes());
//        zipOutputStream.closeEntry();
//      }
//    } catch (IOException ioException){
//      throw new BadRequestException("Could not create file");
//    }
//    return byteArrayOutputStream.toByteArray();
//  }

  Map<String, String> buildSlides(Tournament tournament) {
    Map<String, String> slides = new LinkedHashMap<>();
    for (Event event : tournament.getEvents()) {
          if (event.getEventType() != EventType.DEBATE_SPEAKS) {
            final Optional<EliminationRound> breakLevel = event
                .getResults()
                .stream()
                .map(Result::getEliminationRound)
                .min(Comparator.comparingInt(Enum::ordinal));
            List<Result> collect =
                  event
                      .getResults()
                      .stream()
                      .filter(r -> breakLevel.isPresent() && breakLevel.get() == r.getEliminationRound())
                      .sorted(Comparator.comparing(Result::getCode))
                      .collect(Collectors.toList());
            if(!collect.isEmpty()) {
              slides.put(
                  event.getName(),
                  posting
                      .data("results", collect)
                      .data("event", event)
                      .data("round", breakLevel.map(EliminationRound::getLabel).orElse(""))
                      .data("slideBackground", tournament.getSlideBackgroundUrl())
                      .render()
              );
            }
          }
      }
    return slides;
  }
}
