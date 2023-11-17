package learn.jackson.databind.defaulttyping.polymorphism;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTypeResolverBuilder;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PhysicalTypeIdTest {

    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    @DisplayName("단일 객체 > JSON에 타입 식별자가 포함되지 않을 때, 구상 타입으로 역직렬화 -> 성공")
    void t1() throws JsonProcessingException {
        Vehicle vehicle = new Car("X12345");
        // 다형성

        String json = mapper.writeValueAsString(vehicle);
        assertThat(json).isEqualTo("""
                                       {
                                         "licensePlate" : "X12345"
                                       }""");
        // 직렬화 시 타입 정보가 없음

        Vehicle cachedVehicle = mapper.readValue(json, Car.class);
        // 역직렬화 시 구상 타입(Car)을 정확히 사용하면 인스턴스 생성에 성공한다
        assertThat(((Car) cachedVehicle).getLicensePlate()).isEqualTo("X12345");
    }

    @Test
    @DisplayName("단일 객체 > JSON에 타입 식별자가 포함되지 않을 때, 추상 타입으로 역직렬화 -> 실패")
    void t2() throws JsonProcessingException {
        Vehicle vehicle = new Car("X12345");
        // 다형성, 추상 타입 사용

        String json = mapper.writeValueAsString(vehicle);
        assertThat(json).isEqualTo("""
                                       {
                                         "licensePlate" : "X12345"
                                       }""");
        // 직렬화 시 타입 정보가 없음

        assertThatThrownBy(() -> {
            mapper.readValue(json, Vehicle.class);
            // 역직렬화 시 추상 타입을 사용하면 인스턴스 생성에 실패한다
        }).isInstanceOf(InvalidDefinitionException.class);
        // Cannot construct instance of `learn.jackson.databind.defaulttyping.polymorphism.PhysicalTypeIdTest$Vehicle` (no Creators, like default constructor, exist):
        //  abstract types either need to be mapped to concrete types, have custom deserializer, or contain additional type information
    }

    @Test
    @DisplayName("단일 객체 > JSON에 타입 식별자를 포함시킬 때, 추상 타입으로 역직렬화 -> 성공")
    void t3() throws JsonProcessingException {
        Vehicle vehicle = new Car("X12345");
        // 다형성, 추상 타입 사용

        TypeResolverBuilder<StdTypeResolverBuilder> typer =
            new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL, mapper.getPolymorphicTypeValidator());
        typer = typer.init(Id.CLASS, null);
        typer = typer.inclusion(As.PROPERTY);
        mapper.setDefaultTyping(typer);

        String json = mapper.writeValueAsString(vehicle);
        assertThat(json).isEqualTo("""
                                       {
                                         "@class" : "learn.jackson.databind.defaulttyping.polymorphism.PhysicalTypeIdTest$Car",
                                         "licensePlate" : "X12345"
                                       }""");
        // 직렬화 시 타입 정보가 있음

        Vehicle cachedVehicle = mapper.readValue(json, Vehicle.class);
        // 역직렬화 시 추상 타입을 사용하더라도 인스턴스 생성에 성공한다
        assertThat(((Car) cachedVehicle).getLicensePlate()).isEqualTo("X12345");
    }

    @Test
    @DisplayName("객체 리스트 > JSON에 타입 식별자를 포함시킬 때, 추상 타입으로 역직렬화 -> 실패")
    void t4() throws JsonProcessingException {
        List<Vehicle> vehicles = List.of(new Car("X12345"), new Aeroplane(13));
        // 다형성, 추상 타입 사용

        TypeResolverBuilder<StdTypeResolverBuilder> typer =
            new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL, mapper.getPolymorphicTypeValidator());
        typer = typer.init(Id.CLASS, null);
        typer = typer.inclusion(As.PROPERTY);
        mapper.setDefaultTyping(typer);

        String json = mapper.writeValueAsString(vehicles);
        assertThat(json).isEqualTo("""
                                       [ {
                                         "@class" : "learn.jackson.databind.defaulttyping.polymorphism.PhysicalTypeIdTest$Car",
                                         "licensePlate" : "X12345"
                                       }, {
                                         "@class" : "learn.jackson.databind.defaulttyping.polymorphism.PhysicalTypeIdTest$Aeroplane",
                                         "wingSpan" : 13
                                       } ]""");
        // 직렬화 시 타입 정보가 있음

        assertThatThrownBy(() -> mapper.readValue(json, List.class))
            .isInstanceOf(MismatchedInputException.class);
        // 역직렬화 시 추상 타입을 사용하여 컬렉션을 만들 때 아래 예외 발생
        // Unexpected token (START_OBJECT), expected VALUE_STRING: need String, Number of Boolean value that contains type id (for subtype of java.util.List)
        // 구상 타입에 대한 타입 ID는 존재하지만, 리스트 컬렉션에 대한 타입 ID가 없는 게 문제
    }

    @Test
    @DisplayName("객체 리스트 > JSON에 타입 식별자를 포함시킬 때, 추상 타입으로 역직렬화, List에 대한 믹스인 사용 -> 성공")
    void t5() throws JsonProcessingException {
        List<Vehicle> vehicles = List.of(new Car("X12345"), new Aeroplane(13));
        // 다형성, 추상 타입 사용

        TypeResolverBuilder<StdTypeResolverBuilder> typer =
            new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL, mapper.getPolymorphicTypeValidator());
        typer = typer.init(Id.CLASS, null);
        typer = typer.inclusion(As.PROPERTY);
        mapper.setDefaultTyping(typer);

        mapper.addMixIn(List.class, ListMixIn.class);

        String json = mapper.writeValueAsString(vehicles);
        assertThat(json).isEqualTo("""
                                       [ "java.util.ImmutableCollections$List12", [ {
                                         "@class" : "learn.jackson.databind.defaulttyping.polymorphism.PhysicalTypeIdTest$Car",
                                         "licensePlate" : "X12345"
                                       }, {
                                         "@class" : "learn.jackson.databind.defaulttyping.polymorphism.PhysicalTypeIdTest$Aeroplane",
                                         "wingSpan" : 13
                                       } ] ]""");
        // 단일 객체 및 컬렉션에 대한 타입 정보가 있음

        List<Vehicle> cachedVehicles = mapper.readValue(json, List.class);
        // 역직렬화 시 추상 컬렉션 타입을 사용하더라도 인스턴스 생성에 성공한다
        assertThat(((Car) cachedVehicles.get(0)).getLicensePlate()).isEqualTo("X12345");
        assertThat(((Aeroplane) cachedVehicles.get(1)).getWingSpan()).isEqualTo(13);
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

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class Aeroplane extends Vehicle {

        private int wingSpan;

        public Aeroplane(int wingSpan) {
            this.wingSpan = wingSpan;
        }
    }

    @JsonTypeInfo(use = Id.CLASS)
    interface ListMixIn {

    }
}
