package org.nycfl.certificates.slides;

import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateExtension;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.nycfl.certificates.EliminationRound;
import org.nycfl.certificates.Event;
import org.nycfl.certificates.EventType;
import org.nycfl.certificates.Tournament;
import org.nycfl.certificates.results.Result;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Gatherers;

@ApplicationScoped
@TemplateExtension
public class PostingsBuilder implements BaseAnimatedSlideBuilder {

    private final Template postings;

    private final Template posting;

    @Inject
    public PostingsBuilder(Template postings, Template posting) {
        this.postings = postings;
        this.posting = posting;
    }

    @Override
    public Map<String, String> buildSlides(Tournament tournament) {
        Map<String, String> slides = new LinkedHashMap<>();
        for (Event event : tournament.getEvents()) {
            slides.putAll(renderEvent(event));
        }
        return slides;
    }

    private Map<String, String> renderEvent(Event event) {
        if (event.getEventType() != EventType.DEBATE_SPEAKS) {
            final Optional<EliminationRound> maybeBreakLevel = event
                .getResults()
                .stream()
                .map(Result::getEliminationRound)
                .min(Comparator.comparingInt(Enum::ordinal));

            if (maybeBreakLevel.isEmpty()) return Collections.emptyMap();

            final EliminationRound breakLevel = maybeBreakLevel.get();

            List<Result> highestElimResults =
                event
                    .getResults()
                    .stream()
                    .filter(r -> shouldPost(breakLevel, r))
                    .sorted(Comparator.comparing(Result::getCode))
                    .toList();

            if (!highestElimResults.isEmpty()) {
                final AtomicInteger slideCounter = new AtomicInteger();
                final int i = event.getEntriesPerPostingSlide();
                final Map<EliminationRound, List<Result>> groupedByRound = highestElimResults
                    .stream()
                    .collect(Collectors.groupingBy(Result::getEliminationRound));

                final List<EliminationRound> rounds = groupedByRound.keySet()
                    .stream()
                    .sorted(Comparator.comparing(EliminationRound::ordinal).reversed())
                    .toList();

                return rounds
                    .stream()
                    .flatMap(round -> groupedByRound
                        .get(round)
                        .stream()
                        .gather(Gatherers.windowFixed(i))
                        .map(results ->
                            Map.entry(
                                slideName(event, slideCounter),
                                new PostingSlide(event, round, results).render(posting)
                            )
                        ))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            }
        }
        return Collections.emptyMap();
    }

    private static boolean shouldPost(EliminationRound breakLevel, Result r) {
        if (breakLevel == r.getEliminationRound()) {
            return true;
        } else {
            return breakLevel == EliminationRound.DOUBLE_OCTOFINALIST && r.getEliminationRound() == EliminationRound.PLAY_IN_BEFORE;
        }

    }

    private String slideName(Event event, AtomicInteger slideCounter) {
        return "%s_%s_%d".formatted(
            event.getEventType().name(),
            event.getName(),
            slideCounter.getAndIncrement());
    }

    public String buildSlidesPreview(Tournament tournament) {
        return this.buildSlidesPreview(tournament, postings);
    }

    private record PostingSlide(Event event, EliminationRound round, Collection<Result> results) {
        public String render(Template posting) {
            return posting
                .data("roundType", round.label)
                .data("event", event)
                .data("round", round.label)
                .data("results", results)
                .render();
        }
    }
}
