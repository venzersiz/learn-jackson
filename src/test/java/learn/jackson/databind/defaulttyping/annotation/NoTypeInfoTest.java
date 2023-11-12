package learn.jackson.databind.defaulttyping.annotation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

class NoTypeInfoTest {

    ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void noTypeInfo() throws JsonProcessingException {
        Vehicle vehicle = new Car("68오 8269");

        String json = mapper.writeValueAsString(vehicle);
        assertThat(json).isEqualTo("""
                                       {
                                         "licensePlate" : "68오 8269"
                                       }""");

        Car car = mapper.readValue(json, Car.class);
        assertThat(car.getLicensePlate()).isEqualTo("68오 8269");

        assertThatThrownBy(() -> mapper.readValue(json, Vehicle.class))
            .isInstanceOf(InvalidDefinitionException.class);
        // 아래 예외 발생
        // Cannot construct instance of `learn.jackson.databind.polymorphism.NoTypeInfoTest$Vehicle` (no Creators, like default constructor, exist):
        //  abstract types either need to be mapped to concrete types, have custom deserializer, or contain additional type information
    }

    static abstract class Vehicle {

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class Car extends Vehicle {

        private String licensePlate;

        public Car(String licensePlate) {
            this.licensePlate = licensePlate;
        }
    }
}
