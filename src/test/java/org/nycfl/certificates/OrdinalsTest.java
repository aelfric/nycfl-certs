package org.nycfl.certificates;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class OrdinalsTest {

    @ParameterizedTest(name = "{0} should convert to  {1}")
    @CsvSource({
        "1,First",
        "2,Second",
        "3,Third",
        "20,Twentieth",
        "29,Twenty-Ninth",
        "31,31st"
    })
    @DisplayName("Convert ints to ordinals")
    void test(int input, String expected){
        assertThat(Ordinals.ofInt(input)).isEqualTo(expected);

    }

}