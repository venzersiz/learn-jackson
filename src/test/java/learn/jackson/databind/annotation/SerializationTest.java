package learn.jackson.databind.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

class SerializationTest {

    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void renamingProperty() throws JsonProcessingException {
        @RequiredArgsConstructor
        @Getter
        class Name {

            @JsonProperty("firstName")
            private final String _firstName;
        }

        Name name = new Name("Bob");

        assertThat(mapper.writeValueAsString(name)).isEqualTo("""
                                                                  {
                                                                    "firstName" : "Bob"
                                                                  }""");
    }

    @Test
    void ignoringProperty() throws JsonProcessingException {
        @RequiredArgsConstructor
        @Getter
        @JsonIgnoreProperties({"internalValue", "extra", "uselessValue"})
        class Value {

            private final int value;

            //@JsonIgnore // 필드 단에서 설정도 가능
            private final int internalValue;
        }

        Value value = new Value(42, 100);

        assertThat(mapper.writeValueAsString(value)).isEqualTo("""
                                                                   {
                                                                     "value" : 42
                                                                   }""");
    }
}
