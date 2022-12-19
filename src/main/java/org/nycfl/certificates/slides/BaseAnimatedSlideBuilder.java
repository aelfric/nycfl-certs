package org.nycfl.certificates.slides;

import io.quarkus.qute.Template;
import org.nycfl.certificates.Tournament;

import java.util.Map;

interface BaseAnimatedSlideBuilder {
  default String buildSlidesPreview(Tournament tournament, Template template) {
    String slideBackgroundUrl = tournament.getSlideBackgroundUrl();
    return template.render(
        Map.of(
            "slides",
            buildSlides(tournament)
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .map(Map.Entry::getValue),
            "image", orDefault(slideBackgroundUrl, ""),
            "accentColor", orDefault(tournament.getSlideAccentColor(), "#00356b"),
            "secondaryAccentColor", orDefault(tournament.getSlideSecondaryAccentColor(), "#4a4a4a"),
            "primaryColor", orDefault(tournament.getSlidePrimaryColor(), "#222222"),
            "overlayColor", orDefault(tournament.getSlideOverlayColor(), "#dddddd")
        ));
  }

  private String orDefault(String slideBackgroundUrl, String defaultValue) {
    return slideBackgroundUrl == null ? defaultValue : slideBackgroundUrl;
  }

  Map<String, String> buildSlides(Tournament tournament);
}
