package learn.jackson.databind;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.IOException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

class SimpleTest {

    // 기본적으로 오브젝트 매퍼는 재활용할 수 있다
    ObjectMapper objectMapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT); // Pretty print

    @Test
    void serialize() throws IOException {
        ImmutableUser obj = new ImmutableUser("John", 100);

        // 객체를 JSON 문자열로 만들 수 있고
        String json = objectMapper.writeValueAsString(obj); // throws JsonProcessingException

        assertThat(json).isEqualTo("""
                                       {
                                         "name" : "John",
                                         "age" : 100
                                       }""");

        // JSON 파일로 만들 수도 있다
        // objectMapper.writeValue(new File("src/test/resources/user.json"), json);
        // TODO: 그런데 이스케이프 문자들도 같이 들어가서 역직렬화 시 문제 발생함. 왤까?
        // Expected: {"name" : "John", "age" : 100}
        // Actual: "{\n  \"name\" : \"John\",\n  \"age\" : 100\n}"
    }

    @Test
    void deserialize() throws IOException {
        // ImmutableUser obj = objectMapper.readValue(getClass().getResource("/user.json"), ImmutableUser.class);
        // 기본 생성자가 없어 역직렬화(인스턴스 생성) 불가하여 아래 예외 발생
        // Cannot construct instance of `learn.jackson.simple.DataBindingTest$ImmutableUser` (although at least one Creator exists): cannot deserialize from Object value (no delegate- or property-based Creator)

        // 파일로부터 역직렬화
        User obj = objectMapper.readValue(getClass().getResource("/user.json"), User.class);
        assertThat(obj.getName()).isEqualTo("John");
        assertThat(obj.getAge()).isEqualTo(100);

        // JSON 문자열로부터 역직렬화
        obj = objectMapper.readValue("""
                                         {
                                           "name" : "John",
                                           "age" : 100
                                         }""", User.class);
        assertThat(obj.getName()).isEqualTo("John");
        assertThat(obj.getAge()).isEqualTo(100);
    }

    @RequiredArgsConstructor
    @Getter
    static class ImmutableUser {

        private final String name;

        private final int age;
    }

    @Getter
    // Setter가 없어도 역직렬화가 되는 것을 보면 인스턴스 생성 시 Reflection을 사용하는 것으로 예상된다
    static class User {

        @SuppressWarnings("UnusedDeclaration")
        private String name;

        @SuppressWarnings("UnusedDeclaration")
        private int age;
    }
}
