package org.nycfl.certificates.slides;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.nycfl.certificates.EliminationRound;
import org.nycfl.certificates.Event;
import org.nycfl.certificates.School;
import org.nycfl.certificates.Tournament;
import org.nycfl.certificates.results.Result;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.jupiter.api.Assertions.assertAll;

@QuarkusTest
class SlideBuilderTest {

    @Inject
    SlideBuilder slideBuilder;

    @Inject
    PostingsBuilder postingsBuilder;

    @Inject
    EntityManager entityManager;

    @Test
    void isEmptyWhenNoResults() {
        final Map<String, String> emptySlides = slideBuilder.buildSlides(new Tournament());
        assertThat(emptySlides.values(), empty());

        final Map<String, String> emptyPostings = postingsBuilder.buildSlides(new Tournament());
        assertThat(emptyPostings.values(), empty());
    }

    @Test
    void canPartitionDataForSlides() {
        final Tournament tournament = getTestTournament();
        final Map<String, String> slides = slideBuilder.buildSlides(tournament);
        assertThat(slides.keySet(), hasItems("SPEECH_Duo Interpretation_9_0", "SPEECH_Duo Interpretation_8_0"));
        final String semifinalistSlide = slides.get("SPEECH_Duo Interpretation_8_0");
        assertAll(
            () -> assertThat(semifinalistSlide, containsString("Student 4")),
            () -> assertThat(semifinalistSlide, containsString("Regis High School")),
            () -> assertThat(semifinalistSlide, not(containsString("Student 5")))
        );
    }

    @Test
    void canRenderResultsToSlides() {
        final Tournament tournament = getTestTournament();
        final String html = slideBuilder.buildSlidesPreview(tournament);
        assertAll(
            () -> assertThat(html, containsString("Student 4")),
            () -> assertThat(html, containsString("<style>")),
            () -> assertThat(html, containsString("Regis High School")),
            () -> assertThat(html, containsString("some_image.png")),
            () -> assertThat(html, not(containsString("Student 5")))
        );
    }

    @Test
    void canPartitionDataForPostings() {
        final Tournament tournament = getTestTournament();
        final Map<String, String> slides = postingsBuilder.buildSlides(tournament);
        assertThat(slides.keySet(), hasItems("SPEECH_Duo Interpretation_0"));
        final String semifinalistSlide = slides.get("SPEECH_Duo Interpretation_0");
        assertAll(
            () -> assertThat(semifinalistSlide, containsString("101")),
            () -> assertThat(semifinalistSlide, containsString("102")),
            () -> assertThat(semifinalistSlide, not(containsString("105")))
        );
    }

    @Test
    void canRenderResultsToPostings() {
        final Tournament tournament = getTestTournament();
        final String html = postingsBuilder.buildSlidesPreview(tournament);
        assertAll(
            () -> assertThat(html, containsString(">101<")),
            () -> assertThat(html, containsString(">102<")),
            () -> assertThat(html, containsString(">103<")),
            () -> assertThat(html, containsString("some_image.png")),
            () -> assertThat(html, not(containsString(">105<")))
        );
    }

    private Tournament getTestTournament() {
        final Tournament tournament = new Tournament();
        final Event event = new Event();
        event.setName("Duo Interpretation");
        event.setCertificateCutoff(5);
        event.addResults(List.of(
            makeResult("101", "Student 1", 1, EliminationRound.FINALIST, "Regis High School"),
            makeResult("102", "Student 2", 2, EliminationRound.FINALIST, "Convent NYC"),
            makeResult("103", "Student 3", 3, EliminationRound.FINALIST, "Loyola School"),
            makeResult("104", "Student 4", 4, EliminationRound.SEMIFINALIST, "Regis High School"),
            makeResult("105", "Student 5", 5, EliminationRound.SEMIFINALIST, "Regis High School"),
            makeResult("106", "Student 6", 6, EliminationRound.SEMIFINALIST, "Iona Prep")
        ));
        tournament.setSlideBackgroundUrl("some_image.png");
        tournament.getEvents().add(event);
        return tournament;
    }

    private Result makeResult(String code, String name, int place, EliminationRound eliminationRound, String schoolName) {
        final Result result = new Result();
        result.setName(name);
        result.setCode(code);
        result.setPlace(place);
        result.setEliminationRound(eliminationRound);
        final School school = School.fromName(schoolName);
        result.setSchool(school);
        return result;
    }

}