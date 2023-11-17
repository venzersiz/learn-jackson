package learn.jackson.databind.defaulttyping.polymorphism.intermediate;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;

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
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

class LogicalTypeIdWithCustomTypeIdResolverTest {

    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void dynamicTypeId() throws JsonProcessingException {
        List<Vehicle> vehicles = List.of(new Car("X12345"), new Aeroplane(13));
        RichUser user = new RichUser(vehicles);

        TypeResolverBuilder<StdTypeResolverBuilder> typer =
            new DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL, mapper.getPolymorphicTypeValidator());
        typer = typer.init(Id.NAME, new DynamicTypeIdResolver(List.of("learn.jackson.databind.defaulttyping.polymorphism.intermediate")));
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
        assertThat(((Car) cachedVehicles.get(0)).getLicensePlate()).isEqualTo("X12345");
        assertThat(((Aeroplane) cachedVehicles.get(1)).getWingSpan()).isEqualTo(13);
    }

    static class DynamicTypeIdResolver extends TypeIdResolverBase {

        private final Map<String, Class<?>> classByIdMap;

        public DynamicTypeIdResolver(List<String> basePackages) {

            // 기본 필터는 @Component 기반의 빈들을 등록하기 때문에 사용하지 않음
            ClassPathScanningCandidateComponentProvider componentProvider =
                new ClassPathScanningCandidateComponentProvider(false);
            // @JsonTypeName이 달린 컴포넌트만 사용하도록 필터 추가
            componentProvider.addIncludeFilter(new AnnotationTypeFilter(JsonTypeName.class));

            this.classByIdMap = basePackages.stream()
                                            .map(componentProvider::findCandidateComponents) // 컴포넌트 후보를 클래스 경로에서 찾는다 -> Stream<Set<BeanDefinition>>
                                            .flatMap(Collection::stream) // Flatten -> Stream<BeanDefinition>
                                            .map(BeanDefinition::getBeanClassName) // -> String<String>
                                            .map(className -> {
                                                try {
                                                    return Class.forName(className);
                                                } catch (ClassNotFoundException e) {
                                                    throw new RuntimeException(e);
                                                }
                                            }) // -> Stream<? extends Class<?>>
                                            .collect(toMap(aClass -> aClass.getDeclaredAnnotation(JsonTypeName.class).value(), Function.identity()));
            // @JsonTypeName의 value 요소가 타입 식별자로서 맵의 키가 되고 해당 애너테이션이 달린 모델 클래스가 맵의 값이 된다
        }

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

            return context.constructType(classByIdMap.get(id));
        }
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
    @JsonTypeName("car")
    static class Car extends Vehicle {

        private String licensePlate;

        public Car(String licensePlate) {
            this.licensePlate = licensePlate;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @JsonTypeName("aeroplane")
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
