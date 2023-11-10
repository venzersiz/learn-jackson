package learn.jackson.databind.feature;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import lombok.Getter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DeserializationFeaturesTest {

    @Test
    @DisplayName("기정의한_필드가_아닌_필드가_추가된_경우")
    void t1() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
            {
              "name" : "John",
              "age" : 100
            }""";

        assertThatThrownBy(() -> mapper.readValue(json, User.class))
            .isInstanceOf(UnrecognizedPropertyException.class);
        // 기정의한 필드가 아닌 필드가 추가된 경우 기본적으로 아래 예외 발생
        // Unrecognized field "age" (class learn.jackson.databind.feature.DeserializationFeaturesTest$User), not marked as ignorable (one known property: "name"])

        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.readValue(json, User.class);
    }

    @Getter
    static class User {

        @SuppressWarnings("UnusedDeclaration")
        private String name;
    }

    @Test
    @DisplayName("Object_타입의_값이_빈_공백")
    void t2() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        String json = """
            {
              "user" : ""
            }""";

        // user라는 필드는 Object 타입인데 값이 없다면 null로 정의되어어야 하는데 빈 공백으로 들어오면 아래 예외 발생
        // Cannot coerce empty String ("") to `learn.jackson.databind.feature.DeserializationFeaturesTest$User` value (but could if coercion was enabled using `CoercionConfig`)
        assertThatThrownBy(() -> mapper.readValue(json, InnerWear.class))
            .isInstanceOf(InvalidFormatException.class);

        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        mapper.readValue(json, InnerWear.class);
    }

    @Getter
    static class InnerWear {

        @SuppressWarnings("UnusedDeclaration")
        private User user;
    }
}
