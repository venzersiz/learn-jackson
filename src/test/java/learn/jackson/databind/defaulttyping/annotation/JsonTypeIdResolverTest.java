package learn.jackson.databind.defaulttyping.annotation;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DatabindContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

class JsonTypeIdResolverTest {

    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void jsonTypeIdResolver() throws JsonProcessingException {
        FirstBean bean1 = new FirstBean(1, "Bean 1");
        LastBean bean2 = new LastBean(2, "Bean 2");

        List<AbstractBean> beans = new ArrayList<>();
        beans.add(bean1);
        beans.add(bean2);

        BeanContainer serializedContainer = new BeanContainer(beans);

        String json = mapper.writeValueAsString(serializedContainer);
        assertThat(json).isEqualTo("""
                                       {
                                         "beans" : [ {
                                           "@type" : "bean1",
                                           "id" : 1,
                                           "firstName" : "Bean 1"
                                         }, {
                                           "@type" : "bean2",
                                           "id" : 2,
                                           "lastName" : "Bean 2"
                                         } ]
                                       }""");

        BeanContainer deserializedContainer = mapper.readValue(json, BeanContainer.class);
        beans = deserializedContainer.getBeans();

        assertThat(beans.get(0)).isInstanceOf(FirstBean.class);
        assertThat(beans.get(1)).isInstanceOf(LastBean.class);
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @JsonTypeInfo(use = Id.NAME)
    @JsonTypeIdResolver(BeanIdResolver.class)
    static abstract class AbstractBean {

        private int id;

        public AbstractBean(int id) {
            this.id = id;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class FirstBean extends AbstractBean {

        private String firstName;

        public FirstBean(int id, String firstName) {
            super(id);
            this.firstName = firstName;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class LastBean extends AbstractBean {

        private String lastName;

        public LastBean(int id, String lastName) {
            super(id);
            this.lastName = lastName;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class BeanContainer {

        private List<AbstractBean> beans;

        public BeanContainer(List<AbstractBean> beans) {
            this.beans = beans;
        }
    }

    // TypeIdResolver 인터페이스를 구현하지 말고, TypeIdResolverBase 클래스를 상속하라고 강하게 권장하고 있음
    static class BeanIdResolver extends TypeIdResolverBase {

        private JavaType superType;

        @Override
        public String idFromValue(Object value) {
            return idFromValueAndType(value, value.getClass());
        }

        /**
         * 직렬화 시 타입 정보를 포함하는 로직
         */
        @Override
        public String idFromValueAndType(Object value, Class<?> subType) {
            String typeId = null;

            switch (subType.getSimpleName()) {
                case "FirstBean":
                    typeId = "bean1";
                    break;
                case "LastBean":
                    typeId = "bean2";
                    break;
            }

            return typeId;
        }

        @Override
        public Id getMechanism() {
            return Id.NAME;
        }

        @Override
        public void init(JavaType baseType) {
            this.superType = baseType;
        }

        /**
         * 역직렬화 관련 로직
         */
        @Override
        public JavaType typeFromId(DatabindContext context, String id) {
            Class<?> subType = null;

            switch (id) {
                case "bean1":
                    subType = FirstBean.class;
                    break;
                case "bean2":
                    subType = LastBean.class;
                    break;
            }

            return context.constructSpecializedType(superType, subType);
        }
    }
}
