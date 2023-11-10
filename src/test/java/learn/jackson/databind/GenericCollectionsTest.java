package learn.jackson.databind;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import org.junit.jupiter.api.Test;

class GenericCollectionsTest {

    ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void genericCollections() throws IOException {
        // Map
        Map<String, Integer> scoreByName = mapper.readValue("""
                                                                {
                                                                  "Bob" : 100,
                                                                  "John" : 97
                                                                }""", new TypeReference<>() {
        });
        assertThat(scoreByName).containsEntry("Bob", 100)
                               .containsEntry("John", 97);

        // List
        List<String> names = mapper.readValue("""
                                                  ["Bob", "John"]""", new TypeReference<>() {
        });
        assertThat(names.get(0)).isEqualTo("Bob");
        assertThat(names.get(1)).isEqualTo("John");

        mapper.writeValue(new File("src/test/resources/names.json"), names);

        names = mapper.readValue(getClass().getResource("/names.json"), new TypeReference<>() {
        });
        assertThat(names.get(0)).isEqualTo("Bob");
        assertThat(names.get(1)).isEqualTo("John");

        // Map with value of object type
        Map<String, User> results = mapper.readValue(getClass().getResource("/user-map.json"), new TypeReference<>() {
        });
        assertThat(results.get("Bob").getAge()).isEqualTo(100);
        assertThat(results.get("John").getAge()).isEqualTo(97);
    }

    @Getter
    static class User {

        @SuppressWarnings("UnusedDeclaration")
        private int age;
    }
}
