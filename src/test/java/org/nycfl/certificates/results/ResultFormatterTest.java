package org.nycfl.certificates.results;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.nycfl.certificates.EliminationRound;
import org.nycfl.certificates.Event;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

class ResultFormatterTest {

  @ParameterizedTest
  @CsvSource({
      "1,1,1,FINALIST,Champion",
      "2,2,1,FINALIST,Finalist",
      "6,10,10,FINALIST,Sixth Place",
      "1,10,10,FINALIST,First Place",
      "6,5,10,FINALIST,''",
      "6,10,10,SEMIFINALIST,Semi-Finalist"
  })
  @DisplayName("Debate Results - Placement")
  void testDebateFormatter(
      int place,
      int certificateCutoff,
      int placementCutoff,
      EliminationRound eliminationRound,
      String expected){
    final ResultFormatter formatter = new DebateResultFormatter();

    final Result result = createResult(
        place,
        certificateCutoff,
        placementCutoff,
        eliminationRound);

    assertThat(
        formatter.getPlacementString(result),
        is(expected)
    );
  }

  @ParameterizedTest
  @CsvSource({
      "6,10,10,FINALIST,Sixth Place",
      "1,10,10,FINALIST,First Place",
      "6,5,5,FINALIST,''",
      "6,7,5,FINALIST,Finalist",
      "6,7,5,SEMIFINALIST,Semi-Finalist"
  })
  @DisplayName("Speech Results - Placement")
  void testSpeechFormatter(
      int place,
      int certificateCutoff,
      int placementCutoff,
      EliminationRound eliminationRound,
      String expected){
    final ResultFormatter formatter = new SpeechResultFormatter();

    final Result result = createResult(
        place,
        certificateCutoff,
        placementCutoff,
        eliminationRound);

    assertThat(
        formatter.getPlacementString(result),
        is(expected)
    );
  }
  @ParameterizedTest
  @CsvSource({
      "6,10,10,Sixth Place",
      "1,10,10,First Place",
      "6,5,5,''",
      "6,7,5,''"
  })
  @DisplayName("Debate Top Speaker Results - Placement")
  void testDebateSpeakersFormatter(
      int place,
      int certificateCutoff,
      int placementCutoff,
      String expected){
    final ResultFormatter formatter = new DebateSpeakerResultFormatter();

    final Result result = createResult(
        place,
        certificateCutoff,
        placementCutoff,
        EliminationRound.FINALIST);

    assertThat(
        formatter.getPlacementString(result),
        is(expected)
    );

  }

  @ParameterizedTest
  @CsvSource({
      "6,10,10,FINALIST,Presiding Officer - Finals",
      "1,10,10,FINALIST,Presiding Officer - Finals",
      "6,5,5,FINALIST,Presiding Officer - Finals",
      "6,7,5,SEMIFINALIST,Presiding Officer - Semi-Finals"
  })
  @DisplayName("Congress POs - Placement")
  void testCongressPOFormatter(
      int place,
      int certificateCutoff,
      int placementCutoff,
      EliminationRound eliminationRound,
      String expected){
    final ResultFormatter formatter = new CongressPOResultFormatter();

    final Result result = createResult(
        place,
        certificateCutoff,
        placementCutoff,
        eliminationRound);

    assertThat(
        formatter.getPlacementString(result),
        is(expected)
    );
  }

  private static Result createResult(
      int place,
      int certificateCutoff,
      int placementCutoff,
      EliminationRound eliminationRound) {
    final Event e = new Event();
    e.setCertificateCutoff(certificateCutoff);
    e.setPlacementCutoff(placementCutoff);

    final Result result = new Result();
    result.setPlace(place);
    result.setEvent(e);
    result.setEliminationRound(eliminationRound);

    return result;
  }
}