package learn.jackson.databind.module;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;
import org.junit.jupiter.api.Test;

class Java8DateTest {

    @Test
    void java8Date() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();//.enable(SerializationFeature.INDENT_OUTPUT);
        Time time = new Time();
        time.localDate = LocalDate.of(2023, 11, 10);
        time.localDateTime = time.localDate.atStartOfDay();

        assertThatThrownBy(() -> mapper.writeValueAsString(time))
            .isInstanceOf(InvalidDefinitionException.class);
        // Java 8 date/time type `java.time.LocalDate` not supported by default: add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling (through reference chain: learn.jackson.databind.module.Java8DateTest$Time["localDate"])

        ObjectMapper mapper2 = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper2.registerModule(new JavaTimeModule());
        assertThat(mapper2.writeValueAsString(time))
            .isEqualTo("""
                           {
                             "localDate" : [ 2023, 11, 10 ],
                             "localDateTime" : [ 2023, 11, 10, 0, 0 ]
                           }""");
    }

    @Getter
    static class Time {

        private LocalDate localDate;

        private LocalDateTime localDateTime;
    }
}
