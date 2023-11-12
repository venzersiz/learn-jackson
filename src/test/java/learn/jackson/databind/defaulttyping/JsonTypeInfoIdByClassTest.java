package learn.jackson.databind.defaulttyping;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

class JsonTypeInfoIdByClassTest {

    ObjectMapper mapper = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void idByClass() throws JsonProcessingException {
        Vehicle vehicle = new Car("68오 8269");

        String json = mapper.writeValueAsString(vehicle);
        assertThat(json).isEqualTo("""
                                       {
                                         "@class" : "learn.jackson.databind.defaulttyping.JsonTypeInfoIdByClassTest$Car",
                                         "licensePlate" : "68오 8269"
                                       }""");
        // @JsonTypeInfo의 use 요소의 값으로 타입 식별자를 CLASS로 설정했기 때문에 @class라는 이름의 필드가 자동으로 추가된다. 값은 패키지를 포함한 클래스명이 된다
        // property 요소의 값으로 필드명은 변경 가능하다
        // include 요소의 기본 값은 PROPERTY이므로, JSON 객체 내에서 프라퍼티로 추가되었다

        Car car = mapper.readValue(json, Car.class);
        assertThat(car.getLicensePlate()).isEqualTo("68오 8269");

        Vehicle vehicle2 = mapper.readValue(json, Vehicle.class);
        assertThat(((Car) vehicle2).getLicensePlate()).isEqualTo("68오 8269");
        // 타입 식별자가 존재하기 때문에 상위 타입으로 참조하더라도 문제 없음
    }

    @JsonTypeInfo(use = Id.CLASS)
    @JsonSubTypes({@Type(Car.class), @Type(Aeroplane.class)}) // 역직렬화 시 필요
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

    static class Aeroplane extends Vehicle {

        @SuppressWarnings("UnusedDeclaration")
        private int wingSpan;
    }
}
