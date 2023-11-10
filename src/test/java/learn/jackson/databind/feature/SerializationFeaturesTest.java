package learn.jackson.databind.feature;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SerializationFeaturesTest {

    @Test
    @DisplayName("빈_객체일_때_예외")
    void t1() throws JsonProcessingException {
        @RequiredArgsConstructor
        class User {

            @SuppressWarnings("UnusedDeclaration")
            private final String name;
        }
        User obj = new User("John");

        ObjectMapper mapper = new ObjectMapper();
        assertThatThrownBy(() -> mapper.writeValueAsString(obj))
            .isInstanceOf(InvalidDefinitionException.class);
        // 필드가 존재하지 않아 { }인 상태이면 (User에 Getter가 없음) 아래 예외 발생
        // No serializer found for class learn.jackson.databind.feature.FeaturesTest$1User and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationFeature.FAIL_ON_EMPTY_BEANS)

        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        assertThat(mapper.writeValueAsString(obj)).isEqualTo("{}");
    }

    @Test
    @DisplayName("예쁘게_출력")
    void t2() throws JsonProcessingException {
        Map<String, String> user = new HashMap<>();
        user.put("name", "John");

        ObjectMapper mapper = new ObjectMapper();
        assertThat(mapper.writeValueAsString(user)).isEqualTo("{\"name\":\"John\"}");

        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        assertThat(mapper.writeValueAsString(user)).isEqualTo("""
                                                                  {
                                                                    "name" : "John"
                                                                  }""");
    }

    @Test
    @DisplayName("Date를_Timestamp로_쓰기")
    void t3() throws JsonProcessingException {
        Date date = java.sql.Date.valueOf(LocalDate.of(2023, 11, 10));
        Map<String, Date> map = new HashMap<>();
        map.put("date", date);

        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        // Date, Calendar 타입은 기본적으로 타임스탬프 형태로 직렬화됨
        assertThat(mapper.writeValueAsString(map)).isEqualTo("""
                                                                 {
                                                                   "date" : 1699542000000
                                                                 }""");

        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        assertThat(mapper.writeValueAsString(map)).isEqualTo("""
                                                                 {
                                                                   "date" : "2023-11-10"
                                                                 }""");
    }
}
