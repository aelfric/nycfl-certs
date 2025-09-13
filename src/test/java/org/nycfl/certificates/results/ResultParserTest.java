package org.nycfl.certificates.results;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.nycfl.certificates.CSVUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;

class ResultParserTest {
    final ResultParser rp = (schoolsMap, eliminationRound, inputStream) -> null;

    @Test
    void canHandleAlternateColumnNames() throws Exception {
        try (final CSVParser parse = CSVParser.parse(
            """
                col1,col2
                "abc","def"
                """, CSVUtils.CSV_FORMAT
        )) {
            final CSVRecord strings = parse.getRecords().getFirst();

            assertThat(rp.getOrAlternateColumn(strings, "col3", "col2")).isEqualTo("def");
        }
    }

    @Test
    void canGetNamedColumn() throws Exception {
        try (final CSVParser parse = CSVParser.parse(
            """
                col1,col2
                "abc","def"
                """, CSVUtils.CSV_FORMAT
        )) {
            final CSVRecord strings = parse.getRecords().getFirst();

            assertThat(rp.getOrAlternateColumn(strings, "col1")).isEqualTo("abc");
        }
    }

    @Test
    void failsIfColumnDoesNotExist() throws Exception {
        try (final CSVParser parse = CSVParser.parse(
            """
                col1,col2
                "abc","def"
                """, CSVUtils.CSV_FORMAT
        )) {
            final CSVRecord strings = parse.getRecords().getFirst();

            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> rp.getOrAlternateColumn(strings, "col4", "col5"));
        }
    }

    @Test
    void canProvideFallbackValues() throws Exception {
        try (final CSVParser parse = CSVParser.parse(
            """
                col1,col2
                "abc","def"
                """, CSVUtils.CSV_FORMAT
        )) {
            final CSVRecord strings = parse.getRecords().getFirst();

            assertThat(rp.getOrDefault(strings, "col3", "Default")).isEqualTo("Default");
        }
    }

}