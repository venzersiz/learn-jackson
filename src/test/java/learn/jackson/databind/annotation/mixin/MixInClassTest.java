package learn.jackson.databind.annotation.mixin;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

class MixInClassTest {

    @Test
    void mixIn() throws JsonProcessingException {
        Rectangle rectangle = new Rectangle(10, 10);

        ObjectMapper mapper1 = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        assertThat(mapper1.writeValueAsString(rectangle)).isEqualTo("""
                                                                        {
                                                                          "w" : 10,
                                                                          "h" : 10,
                                                                          "size" : 100
                                                                        }""");

        ObjectMapper mapper2 = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper2.addMixIn(Rectangle.class, MixIn.class);
        // Rectangle 클래스에 추상 클래스 믹스인
        assertThat(mapper2.writeValueAsString(rectangle)).isEqualTo("""
                                                                        {
                                                                          "width" : 10,
                                                                          "height" : 10
                                                                        }""");
        // 대상 클래스의 소스 코드 수정 없이 (예를 들어 라이브러리 코드) MixIn을 통해 직렬화를 변경할 수 있다
    }

    @RequiredArgsConstructor
    @Getter
    static final class Rectangle {

        private final int w;

        private final int h;

        @SuppressWarnings("UnusedDeclaration")
        public int getSize() {
            return w * h;
        }
    }

    static abstract class MixIn {

        @SuppressWarnings("UnusedDeclaration")
        public MixIn(@JsonProperty("width") int w, @JsonProperty("height") int h) {
        }

        @JsonProperty("width")
        abstract int getW();

        @JsonProperty("height")
        abstract int getH();

        @JsonIgnore
        abstract int getSize();
    }
}
