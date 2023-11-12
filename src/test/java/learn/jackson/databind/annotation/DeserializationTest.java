package learn.jackson.databind.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

class DeserializationTest {

    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void usingConstructorOrFactoryMethod() throws JsonProcessingException {
        String json = """
            {
              "x" : 1,
              "y" : 2
            }""";

        assertThatThrownBy(() -> mapper.readValue(json, Point.class))
            .isInstanceOf(InvalidDefinitionException.class);
        // 불변 클래스인 Point는 기본 생성자가 없다. 잭슨은 기본적으로 역직렬화 시 기본 생성자를 사용하기 때문에 아래 예외 발생
        // Cannot construct instance of `learn.jackson.annotation.IntermediateTest$ImmutablePoint` (no Creators, like default constructor, exist): cannot deserialize from Object value (no delegate- or property-based Creator)

        // 하지만 아래와 같이 파라미터가 있는 생성자 또는 팩터리 매세드를 사용할 수도 있다
        Point2 point2 = mapper.readValue(json, Point2.class);
        assertThat(point2.getX()).isEqualTo(1);
        assertThat(point2.getY()).isEqualTo(2);
    }

    @RequiredArgsConstructor
    @Getter
    static class Point {

        private final int x;

        private final int y;
    }

    /**
     * Point2 클래스는 불변 객체이지만 역직렬화 가능하다. 다만 코드가 지저분해진다. 으으..
     */
    @Getter
    static class Point2 {

        private final int x;

        private final int y;

        // @JsonProperty를 명시하지 않으면 아래 예외 발생
        // Invalid type definition for type `learn.jackson.databind.annotation.DeserializationTest$Point2`:
        //  Argument #0 of constructor [constructor for `learn.jackson.databind.annotation.DeserializationTest$Point2` (2 args), annotations: {interface com.fasterxml.jackson.annotation.JsonCreator=@com.fasterxml.jackson.annotation.JsonCreator(mode=DEFAULT)} has no property name (and is not Injectable): can not use as property-based Creator

        @JsonCreator
        public Point2(@JsonProperty("x") int x, @JsonProperty("y") int y) {
            this.x = x;
            this.y = y;
        }

        @JsonCreator
        public static Point2 create(@JsonProperty("x") int x, @JsonProperty("y") int y) {
            return new Point2(x, y);
        }
    }
}
