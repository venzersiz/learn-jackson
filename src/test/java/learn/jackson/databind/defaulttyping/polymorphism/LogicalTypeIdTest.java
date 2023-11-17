package learn.jackson.databind.defaulttyping.polymorphism;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTypeResolverBuilder;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LogicalTypeIdTest {

    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    @DisplayName("단일 객체 > JSON에 타입 식별자를 포함시키지만 직렬화 데이터 모델과 역직렬화 데이터 모델의 패키지나 클래스명이 다를 때 -> 실패")
    void t1() throws JsonProcessingException {
        Vehicle vehicle = new Car("X12345");

        TypeResolverBuilder<StdTypeResolverBuilder> typer =
            new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL, mapper.getPolymorphicTypeValidator());
        typer = typer.init(Id.CLASS, null);
        typer = typer.inclusion(As.PROPERTY);
        mapper.setDefaultTyping(typer);

        String json = mapper.writeValueAsString(vehicle);
        assertThat(json).isEqualTo("""
                                       {
                                         "@class" : "learn.jackson.databind.defaulttyping.polymorphism.LogicalTypeIdTest$Car",
                                         "licensePlate" : "X12345"
                                       }""");
        // 직렬화 시 직렬화 데이터 모델의 물리적인 타입 정보가 있음

        Vehicle cachedVehicle = mapper.readValue(json, Vehicle.class);
        // 역직렬화 시 동일한 데이터 구조를 가지더라도 패키지나 클래스명이 다르면 인스턴스 생성에 실패한다
        assertThatThrownBy(() -> ((Car2) cachedVehicle).getLicensePlate())
            .isInstanceOf(ClassCastException.class);
    }

    @Test
    @DisplayName("단일 객체 > 논리적인 타입 식별자를 사용하면 직렬화 데이터 모델과 역직렬화 데이터 모델의 패키지나 클래스명이 다를 때 -> 성공")
    void t2() throws JsonProcessingException {
        Vehicle vehicle = new CarWithJsonTypeName("X12345");
        // @JsonTypeName이 달린 하위 인스턴스 생성

        TypeResolverBuilder<StdTypeResolverBuilder> typer =
            new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL, mapper.getPolymorphicTypeValidator());
        typer = typer.init(Id.NAME, new TypeIdResolverBase() { // 커스텀 TypeIdResolver 등록

            @Override
            public String idFromValue(Object value) {
                return idFromValueAndType(value, value.getClass());
            }

            @Override
            public String idFromValueAndType(Object value, Class<?> suggestedType) {
                // @JsonTypeName의 value 요소의 값이 타입 식별자가 되도록 함
                return suggestedType.getDeclaredAnnotation(JsonTypeName.class).value();
            }

            @Override
            public Id getMechanism() {
                return null; // 딱히 이 메서드의 로직이 사용되지는 않는 것 같음
            }

            @Override
            public JavaType typeFromId(DatabindContext context, String id) {

                Class<?> type = null;

                // 논리적인 타입 식별자를 보고 실제 타입을 매핑
                switch (id) {
                    case "car":
                        type = CarWithJsonTypeName.class;
                        break;
                }

                return context.constructType(type);
            }
        });
        typer = typer.inclusion(As.PROPERTY);
        mapper.setDefaultTyping(typer);

        String json = mapper.writeValueAsString(vehicle);
        assertThat(json).isEqualTo("""
                                       {
                                         "@type" : "car",
                                         "licensePlate" : "X12345"
                                       }""");
        // 직렬화 시 직렬화 데이터 모델의 논리적인 타입 정보가 있음

        Vehicle cachedVehicle = mapper.readValue(json, Vehicle.class);
        assertThat(((CarWithJsonTypeName) cachedVehicle).getLicensePlate()).isEqualTo("X12345");
        // 이제 구체적인 패키지명, 클래스명이 없더라도 다형성을 온전히 지원할 수 있다
    }

    @Test
    @DisplayName("객체 리스트 > 논리적인 타입 식별자 사용 -> 실패")
    void t3() throws JsonProcessingException {
        List<Vehicle> vehicles = List.of(new CarWithJsonTypeName("X12345"), new AeroplaneWithJsonType(13));

        TypeResolverBuilder<StdTypeResolverBuilder> typer =
            new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL, mapper.getPolymorphicTypeValidator());
        typer = typer.init(Id.NAME, new TypeIdResolverBase() {

            @Override
            public String idFromValue(Object value) {
                return idFromValueAndType(value, value.getClass());
            }

            @Override
            public String idFromValueAndType(Object value, Class<?> suggestedType) {
                return suggestedType.getDeclaredAnnotation(JsonTypeName.class).value();
            }

            @Override
            public Id getMechanism() {
                return null;
            }

            @Override
            public JavaType typeFromId(DatabindContext context, String id) {

                Class<?> type = null;

                switch (id) {
                    case "car":
                        type = CarWithJsonTypeName.class;
                        break;
                    case "aeroplane":
                        type = AeroplaneWithJsonType.class;
                        break;
                }

                return context.constructType(type);
            }
        });
        typer = typer.inclusion(As.PROPERTY);
        mapper.setDefaultTyping(typer);

        String json = mapper.writeValueAsString(vehicles);
        assertThat(json).isEqualTo("""
                                       [ {
                                         "@type" : "car",
                                         "licensePlate" : "X12345"
                                       }, {
                                         "@type" : "aeroplane",
                                         "wingSpan" : 13
                                       } ]""");

        assertThatThrownBy(() -> mapper.readValue(json, List.class))
            .isInstanceOf(MismatchedInputException.class);
        // 역직렬화 시 추상 타입을 사용하여 컬렉션을 만들 때 아래 예외 발생
        // Unexpected token (START_OBJECT), expected VALUE_STRING:
        //  need String, Number of Boolean value that contains type id (for subtype of java.util.List)
        // 구상 타입에 대한 타입 ID는 존재하지만, 리스트 컬렉션에 대한 타입 ID가 없는 게 문제
    }

    @Test
    @DisplayName("객체 리스트 > 논리적인 타입 식별자 사용, List 컬렉션에 대한 타입 식별자를 알려주는 믹스인 사용 -> 성공")
    void t4() throws JsonProcessingException {
        List<Vehicle> vehicles = List.of(new CarWithJsonTypeName("X12345"), new AeroplaneWithJsonType(13));

        TypeResolverBuilder<StdTypeResolverBuilder> typer =
            new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL, mapper.getPolymorphicTypeValidator());
        typer = typer.init(Id.NAME, new TypeIdResolverBase() {

            @Override
            public String idFromValue(Object value) {
                return idFromValueAndType(value, value.getClass());
            }

            @Override
            public String idFromValueAndType(Object value, Class<?> suggestedType) {
                return suggestedType.getDeclaredAnnotation(JsonTypeName.class).value();
            }

            @Override
            public Id getMechanism() {
                return null;
            }

            @Override
            public JavaType typeFromId(DatabindContext context, String id) {

                Class<?> type = null;

                switch (id) {
                    case "car":
                        type = CarWithJsonTypeName.class;
                        break;
                    case "aeroplane":
                        type = AeroplaneWithJsonType.class;
                        break;
                }

                return context.constructType(type);
            }
        });
        typer = typer.inclusion(As.PROPERTY);
        mapper.setDefaultTyping(typer);

        mapper.addMixIn(List.class, ListMixIn.class);
        // List 코드를 직접 수정할 수 없으니 믹스인 인터페이스를 사용하여 List의 구상 타입을 직렬화할 수 있게 한다

        String json = mapper.writeValueAsString(vehicles);
        assertThat(json).isEqualTo("""
                                       [ "java.util.ImmutableCollections$List12", [ {
                                         "@type" : "car",
                                         "licensePlate" : "X12345"
                                       }, {
                                         "@type" : "aeroplane",
                                         "wingSpan" : 13
                                       } ] ]""");

        List<Vehicle> cachedVehicles = mapper.readValue(json, List.class);
        // 역직렬화 시 추상 컬렉션 타입을 사용하더라도 인스턴스 생성에 성공한다
        assertThat(((CarWithJsonTypeName) cachedVehicles.get(0)).getLicensePlate()).isEqualTo("X12345");
        assertThat(((AeroplaneWithJsonType) cachedVehicles.get(1)).getWingSpan()).isEqualTo(13);
    }

    @Test
    @DisplayName("객체 그래프 -> 성공")
    void t5() throws JsonProcessingException {
        List<Vehicle> vehicles = List.of(new CarWithJsonTypeName("X12345"), new AeroplaneWithJsonType(13));
        RichUser user = new RichUser(vehicles);

        TypeResolverBuilder<StdTypeResolverBuilder> typer =
            new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL, mapper.getPolymorphicTypeValidator());
        typer = typer.init(Id.NAME, new TypeIdResolverBase() {

            @Override
            public String idFromValue(Object value) {
                return idFromValueAndType(value, value.getClass());
            }

            @Override
            public String idFromValueAndType(Object value, Class<?> suggestedType) {
                return suggestedType.getDeclaredAnnotation(JsonTypeName.class).value();
            }

            @Override
            public Id getMechanism() {
                return null;
            }

            @Override
            public JavaType typeFromId(DatabindContext context, String id) {

                Class<?> type = null;

                switch (id) {
                    case "car":
                        type = CarWithJsonTypeName.class;
                        break;
                    case "aeroplane":
                        type = AeroplaneWithJsonType.class;
                        break;
                    case "richUser":
                        type = RichUser.class;
                        break;
                }

                return context.constructType(type);
            }
        });
        typer = typer.inclusion(As.PROPERTY);
        mapper.setDefaultTyping(typer);

        mapper.addMixIn(List.class, ListMixIn.class);

        String json = mapper.writeValueAsString(user);
        assertThat(json).isEqualTo("""
                                       {
                                         "@type" : "richUser",
                                         "vehicles" : [ "java.util.ImmutableCollections$List12", [ {
                                           "@type" : "car",
                                           "licensePlate" : "X12345"
                                         }, {
                                           "@type" : "aeroplane",
                                           "wingSpan" : 13
                                         } ] ]
                                       }""");

        List<Vehicle> cachedVehicles = mapper.readValue(json, RichUser.class).getVehicles();
        assertThat(((CarWithJsonTypeName) cachedVehicles.get(0)).getLicensePlate()).isEqualTo("X12345");
        assertThat(((AeroplaneWithJsonType) cachedVehicles.get(1)).getWingSpan()).isEqualTo(13);
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @JsonTypeName("richUser")
    static class RichUser {

        private List<Vehicle> vehicles;

        public RichUser(List<Vehicle> vehicles) {
            this.vehicles = vehicles;
        }
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
    static class Car2 extends Vehicle {

        private String licensePlate;

        public Car2(String licensePlate) {
            this.licensePlate = licensePlate;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @JsonTypeName("car")
    static class CarWithJsonTypeName extends Vehicle {

        private String licensePlate;

        public CarWithJsonTypeName(String licensePlate) {
            this.licensePlate = licensePlate;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @JsonTypeName("aeroplane")
    static class AeroplaneWithJsonType extends Vehicle {

        private int wingSpan;

        public AeroplaneWithJsonType(int wingSpan) {
            this.wingSpan = wingSpan;
        }
    }

    @JsonTypeInfo(use = Id.CLASS) // 페키지 + 클래스명을 타입 식별자로 설정
    interface ListMixIn {

    }
}
