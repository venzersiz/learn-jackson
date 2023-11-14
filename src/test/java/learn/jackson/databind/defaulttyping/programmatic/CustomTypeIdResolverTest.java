package learn.jackson.databind.defaulttyping.programmatic;

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
import com.fasterxml.jackson.databind.type.SimpleType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

class CustomTypeIdResolverTest {

    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void jsonTypeIdResolver() throws JsonProcessingException {
        TypeResolverBuilder<StdTypeResolverBuilder> typer = new DefaultTypeResolverBuilder(DefaultTyping.EVERYTHING);
        typer = typer.init(Id.NAME, new CustomTypeIdResolver(List.of("learn.jackson.databind.defaulttyping.programmatic")));
        typer = typer.inclusion(As.PROPERTY);

        mapper.setDefaultTyping(typer);

        mapper.addMixIn(List.class, ListMixIn.class);
        // 위 믹스인 코드를 넣지 않으면 아래 예외 발생
        // com.fasterxml.jackson.databind.exc.MismatchedInputException:
        //  Cannot deserialize value of type `java.util.ArrayList` from Array value (token `JsonToken.START_ARRAY`)
        // 이유 추측: 위에서 설정한 TypeResolver를 통해 타입 식별자를 NAME 유형으로 전역 설정되어, java.util.ArrayList이 아닌 ArrayList로 등록되어 클래스를 찾을 수 없음
        // 그래서 List 인터페이스에 대해 믹스인을 통해 CLASS 유형으로 동작하도록 하였음

        FirstBean bean1 = new FirstBean(1, "Bean 1");
        LastBean bean2 = new LastBean(2, "Bean 2");

        List<AbstractBean> beans = new ArrayList<>();
        beans.add(bean1);
        beans.add(bean2);

        BeanContainer serializedContainer = new BeanContainer(beans);

        String json = mapper.writeValueAsString(serializedContainer);
        assertThat(json).isEqualTo("""
                                       {
                                         "@type" : "BeanContainer",
                                         "beans" : [ "java.util.ArrayList", [ {
                                           "@type" : "FirstBean",
                                           "id" : 1,
                                           "firstName" : "Bean 1"
                                         }, {
                                           "@type" : "LastBean",
                                           "id" : 2,
                                           "lastName" : "Bean 2"
                                         } ] ]
                                       }""");

        BeanContainer deserializedContainer = mapper.readValue(json, BeanContainer.class);
        beans = deserializedContainer.getBeans();

        assertThat(beans.get(0)).isInstanceOf(FirstBean.class);
        assertThat(beans.get(1)).isInstanceOf(LastBean.class);
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static abstract class AbstractBean {

        private int id;

        public AbstractBean(int id) {
            this.id = id;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @JsonTypeName("FirstBean")
    static class FirstBean extends AbstractBean {

        private String firstName;

        public FirstBean(int id, String firstName) {
            super(id);
            this.firstName = firstName;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @JsonTypeName("LastBean")
    static class LastBean extends AbstractBean {

        private String lastName;

        public LastBean(int id, String lastName) {
            super(id);
            this.lastName = lastName;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @JsonTypeName("BeanContainer")
    static class BeanContainer {

        private List<AbstractBean> beans;

        public BeanContainer(List<AbstractBean> beans) {
            this.beans = beans;
        }
    }

    static class CustomTypeIdResolver extends TypeIdResolverBase {

        private Map<String, Class> classByIdMap;

        public CustomTypeIdResolver(List<String> basePackages) {
            // 기본 필터는 @Component 기반의 빈들을 등록하기 때문에 사용하지 않음
            ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(false);
            // @JsonTypeName이 달린 컴포넌트만 사용하도록 필터 추가
            componentProvider.addIncludeFilter(new AnnotationTypeFilter(JsonTypeName.class));

            classByIdMap = basePackages.stream()
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
                                       .collect(toMap(aClass -> aClass.getDeclaredAnnotation(JsonTypeName.class).value(), aClass -> aClass));
        }

        @Override
        public String idFromValue(Object value) {
            return idFromValueAndType(value, value.getClass());
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> suggestedType) {

            JsonTypeName typeName = suggestedType.getDeclaredAnnotation(JsonTypeName.class);
            return typeName.value();
        }

        @Override
        public Id getMechanism() {
            return Id.NAME;
        }

        @Override
        public JavaType typeFromId(DatabindContext context, String id) {

            Class aClass = classByIdMap.computeIfAbsent(id, key -> {
                try {
                    return Class.forName(id);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException(e);
                }
            });

            return SimpleType.constructUnsafe(aClass);
        }
    }

    @JsonTypeInfo(use = Id.CLASS)
    interface ListMixIn {

    }
}
