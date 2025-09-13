package org.nycfl.certificates.slides;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.nycfl.certificates.EliminationRound;
import org.nycfl.certificates.Event;
import org.nycfl.certificates.School;
import org.nycfl.certificates.Tournament;
import org.nycfl.certificates.results.Result;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@QuarkusTest
class SlideBuilderTest {

    @Inject
    SlideBuilder slideBuilder;

    @Inject
    PostingsBuilder postingsBuilder;

    @Test
    void isEmptyWhenNoResults() {
        assertThat(slideBuilder.buildSlides(new Tournament())).isEmpty();
        assertThat(postingsBuilder.buildSlides(new Tournament())).isEmpty();
    }

    @Test
    void canPartitionDataForSlides() {
        final Tournament tournament = getTestTournament();
        final Map<String, String> slides = slideBuilder.buildSlides(tournament);
        assertThat(slides)
            .containsKeys("SPEECH_Duo Interpretation_9_0", "SPEECH_Duo Interpretation_8_0");
        assertThat(
            slides.get("SPEECH_Duo Interpretation_8_0")
        )
            .contains("Student 4")
            .contains("Regis High School")
            .doesNotContain("Student 5");
    }

    @Test
    void canRenderResultsToSlides() {
        final Tournament tournament = getTestTournament();
        assertThat(slideBuilder.buildSlidesPreview(tournament))
            .contains("Student 4")
            .contains("<style>")
            .contains("Regis High School")
            .contains("some_image.png")
            .doesNotContain("Student 5");
    }

    @Test
    void canPartitionDataForPostings() {
        final Tournament tournament = getTestTournament();
        final Map<String, String> slides = postingsBuilder.buildSlides(tournament);
        assertThat(slides).containsKey("SPEECH_Duo Interpretation_0");
        assertThat(
            slides.get("SPEECH_Duo Interpretation_0")
        )
            .contains("101")
            .contains("102")
            .doesNotContain("105");
    }

    @Test
    void canRenderResultsToPostings() {
        final Tournament tournament = getTestTournament();
        assertThat(
            postingsBuilder.buildSlidesPreview(tournament)
        )
            .contains(">101<")
            .contains(">102<")
            .contains(">103<")
            .contains("some_image.png")
            .doesNotContain(">105<");
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