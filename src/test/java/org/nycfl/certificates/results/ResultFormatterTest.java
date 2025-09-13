package org.nycfl.certificates.results;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.nycfl.certificates.EliminationRound;
import org.nycfl.certificates.Event;

import static org.assertj.core.api.Assertions.assertThat;

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
  void debateFormatter(
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

      assertThat(formatter.getPlacementString(result)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "1,1,1,FINALIST,gold",
      "2,2,1,FINALIST,silver",
      "6,10,10,SEMIFINALIST,bronze",
      "1,10,10,FINALIST,gold",
      "6,10,10,QUARTER_FINALIST,red"
  })
  @DisplayName("Debate Results - Color")
  void debateFormatterColor(
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
        eliminationRound
    );

      assertThat(formatter.getCertificateColor(result)).isEqualTo(expected);
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
  void speechFormatter(
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

      assertThat(formatter.getPlacementString(result)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "6,10,10,FINALIST,red finalist",
      "1,10,10,FINALIST,gold finalist",
      "2,10,10,FINALIST,silver finalist",
      "3,10,10,FINALIST,bronze finalist",
      "4,7,5,FINALIST,red finalist",
      "6,7,5,FINALIST,black finalist",
      "6,7,5,SEMIFINALIST,black semi-finalist"
  })
  @DisplayName("Speech Results - Color")
  void speechFormatterColor(
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

      assertThat(formatter.getCertificateColor(result)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "6,10,10,Sixth Place",
      "1,10,10,First Place",
      "6,5,5,''",
      "6,7,5,''"
  })
  @DisplayName("Debate Top Speaker Results - Placement")
  void debateSpeakersFormatter(
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

      assertThat(formatter.getPlacementString(result)).isEqualTo(expected);

  }@ParameterizedTest
  @CsvSource({
      "6,10,10,red",
      "1,10,10,gold",
      "2,10,10,silver",
      "3,10,10,bronze",
      "6,5,5,black",
      "6,7,5,black"
  })
  @DisplayName("Debate Top Speaker Results - Color")
  void debateSpeakersFormatterColor(
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

    assertThat(formatter.getCertificateColor(result)).isEqualTo(expected);

  }

  @ParameterizedTest
  @CsvSource({
      "6,10,10,FINALIST,Presiding Officer - Finals",
      "1,10,10,FINALIST,Presiding Officer - Finals",
      "6,5,5,FINALIST,Presiding Officer - Finals",
      "6,7,5,SEMIFINALIST,Presiding Officer - Semi-Finals"
  })
  @DisplayName("Congress POs - Placement")
  void congressPOFormatter(
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

      assertThat(formatter.getPlacementString(result)).isEqualTo(expected);
  }

  @ParameterizedTest
  @CsvSource({
      "6,10,10,FINALIST,po finalist",
      "6,7,10,SEMIFINALIST,po semi-finalist"
  })
  @DisplayName("Congress POs - Color")
  void congressPOFormatterColor(
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

      assertThat(formatter.getCertificateColor(result)).isEqualTo(expected);
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