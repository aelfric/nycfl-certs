package org.nycfl.certificates.results;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;
import org.nycfl.certificates.CSVUtils;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultParserTest {
    final ResultParser rp = (schoolsMap, eliminationRound, inputStream) -> null;

    @Test
    void canHandleAlternateColumnNames() throws IOException {
        try (final CSVParser parse = CSVParser.parse(
            """
                col1,col2
                "abc","def"
                """, CSVUtils.CSV_FORMAT
        )) {
            final CSVRecord strings = parse.getRecords().get(0);

            assertThat(
                rp.getOrAlternateColumn(strings, "col3", "col2"),
                CoreMatchers.is("def")
            );
        }
    }

    @Test
    void canGetNamedColumn() throws IOException {
        try (final CSVParser parse = CSVParser.parse(
            """
                col1,col2
                "abc","def"
                """, CSVUtils.CSV_FORMAT
        )) {
            final CSVRecord strings = parse.getRecords().get(0);

            assertThat(
                rp.getOrAlternateColumn(strings, "col1"),
                CoreMatchers.is("abc")
            );
        }
    }

    @Test
    void failsIfColumnDoesNotExist() throws IOException {
        try (final CSVParser parse = CSVParser.parse(
            """
                col1,col2
                "abc","def"
                """, CSVUtils.CSV_FORMAT
        )) {
            final CSVRecord strings = parse.getRecords().get(0);

            assertThrows(
                IllegalArgumentException.class,
                () -> rp.getOrAlternateColumn(strings, "col4", "col5")
            );
        }
    }

    @Test
    void canProvideFallbackValues() throws IOException {
        try (final CSVParser parse = CSVParser.parse(
            """
                col1,col2
                "abc","def"
                """, CSVUtils.CSV_FORMAT
        )) {
            final CSVRecord strings = parse.getRecords().get(0);

            assertThat(
                rp.getOrDefault(strings, "col3", "Default"),
                CoreMatchers.is("Default")
            );
        }
    }

}