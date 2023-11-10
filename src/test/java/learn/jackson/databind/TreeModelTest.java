package learn.jackson.databind;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.Test;

class TreeModelTest {

    ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void treeModel() throws JsonProcessingException {
        JsonNode root = mapper.readTree("""
                                            {
                                              "name" : "Joe",
                                              "age" : 13
                                            }""");
        assertThat(root.get("name").asText()).isEqualTo("Joe");
        assertThat(root.get("age").asInt()).isEqualTo(13);

        root.withObject("/other").put("type", "student");
        assertThat(mapper.writeValueAsString(root)).isEqualTo("""
                                                                  {
                                                                    "name" : "Joe",
                                                                    "age" : 13,
                                                                    "other" : {
                                                                      "type" : "student"
                                                                    }
                                                                  }""");
    }
}
