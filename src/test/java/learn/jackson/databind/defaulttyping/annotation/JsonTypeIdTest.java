package learn.jackson.databind.defaulttyping.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonTypeId;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

class JsonTypeIdTest {

    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT)
                                            .activateDefaultTyping(BasicPolymorphicTypeValidator.builder().build(),
                                                                   DefaultTyping.NON_FINAL,
                                                                   As.PROPERTY);

    @Test
    void noJsonTypeId() throws JsonProcessingException {
        @RequiredArgsConstructor
        @Getter
        class User {

            private final String name;

            private final int age;
        }

        User user = new User("John", 100);

        assertThat(mapper.writeValueAsString(user)).isEqualTo("""
                                                                  {
                                                                    "@class" : "learn.jackson.databind.defaulttyping.programmatic.JsonTypeIdTest$1User",
                                                                    "name" : "John",
                                                                    "age" : 100
                                                                  }""");
    }

    @Test
    void jsonTypeId() throws JsonProcessingException {
        @RequiredArgsConstructor
        @Getter
        class User {

            // 해당 필드가 타입 식별자가 되며, 기존 필드는 사라짐
            @JsonTypeId
            private final String name;

            private final int age;
        }

        User user = new User("John", 100);

        assertThat(mapper.writeValueAsString(user)).isEqualTo("""
                                                                  {
                                                                    "@class" : "John",
                                                                    "age" : 100
                                                                  }""");
    }
}
