package learn.jackson.databind.annotation.mixin;

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
import com.fasterxml.jackson.databind.exc.InvalidTypeIdException;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Test;

class MixInInterfaceTest {

    ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    void noTypeId() throws JsonProcessingException {
        List<Address> addresses = new ArrayList<>();
        addresses.add(new Address("주소1"));
        addresses.add(new Address("주소2"));
        Member member = new Member("아무개", addresses);

        String json = mapper.writeValueAsString(member);
        assertThat(json).isEqualTo("""
                                       {
                                         "name" : "아무개",
                                         "addresses" : [ {
                                           "name" : "주소1"
                                         }, {
                                           "name" : "주소2"
                                         } ]
                                       }""");

        Member deserializedMember = mapper.readValue(json, Member.class);
        assertThat(deserializedMember.getName()).isEqualTo("아무개");

        List<Address> deserializedAddresses = deserializedMember.getAddresses();
        assertThat(deserializedAddresses.get(0).getName()).isEqualTo("주소1");
        assertThat(deserializedAddresses.get(1).getName()).isEqualTo("주소2");
        // 타입 식별자가 없을 때는 역직렬화 시 아무 문제가 없다
    }

    @Test
    void classTypeIdExists() throws JsonProcessingException {
        List<Address> addresses = new ArrayList<>();
        addresses.add(new Address("주소1"));
        addresses.add(new Address("주소2"));
        Member member = new Member("아무개", addresses);

        TypeResolverBuilder<StdTypeResolverBuilder> typer =
            new DefaultTypeResolverBuilder(DefaultTyping.EVERYTHING, mapper.getPolymorphicTypeValidator());
        typer = typer.init(Id.CLASS, null);
        typer = typer.inclusion(As.PROPERTY);
        mapper.setDefaultTyping(typer);

        String json = mapper.writeValueAsString(member);
        assertThat(json).isEqualTo("""
                                       {
                                         "@class" : "learn.jackson.databind.annotation.mixin.MixInInterfaceTest$Member",
                                         "name" : "아무개",
                                         "addresses" : [ "java.util.ArrayList", [ {
                                           "@class" : "learn.jackson.databind.annotation.mixin.MixInInterfaceTest$Address",
                                           "name" : "주소1"
                                         }, {
                                           "@class" : "learn.jackson.databind.annotation.mixin.MixInInterfaceTest$Address",
                                           "name" : "주소2"
                                         } ] ]
                                       }""");
        // 직렬화 시 List의 경우 WRAPPER_ARRAY 방식으로 실제 데이터를 배열로 한 번 더 감싼다
        // List의 다형성 구현을 위해 패키지명을 포함하는 클래스명이 타입 식별자가 된다

        Member deserializedMember = mapper.readValue(json, Member.class);
        assertThat(deserializedMember.getName()).isEqualTo("아무개");

        List<Address> deserializedAddresses = deserializedMember.getAddresses();
        assertThat(deserializedAddresses.get(0).getName()).isEqualTo("주소1");
        assertThat(deserializedAddresses.get(1).getName()).isEqualTo("주소2");
    }

    @Test
    void nameTypeIdExistsAndDeserializationFail() throws JsonProcessingException {
        List<Address> addresses = new ArrayList<>();
        addresses.add(new Address("주소1"));
        addresses.add(new Address("주소2"));
        Member member = new Member("아무개", addresses);

        TypeResolverBuilder<StdTypeResolverBuilder> typer =
            new DefaultTypeResolverBuilder(DefaultTyping.EVERYTHING, mapper.getPolymorphicTypeValidator());
        typer = typer.init(Id.NAME, null);
        typer = typer.inclusion(As.PROPERTY);
        mapper.setDefaultTyping(typer);

        String json = mapper.writeValueAsString(member);
        assertThat(json).isEqualTo("""
                                       {
                                         "@type" : "MixInInterfaceTest$Member",
                                         "name" : "아무개",
                                         "addresses" : [ "ArrayList", [ {
                                           "@type" : "MixInInterfaceTest$Address",
                                           "name" : "주소1"
                                         }, {
                                           "@type" : "MixInInterfaceTest$Address",
                                           "name" : "주소2"
                                         } ] ]
                                       }""");

        assertThatThrownBy(() -> mapper.readValue(json, Member.class)).isInstanceOf(InvalidTypeIdException.class);
        // 직렬화 시 List의 경우 WRAPPER_ARRAY 방식으로 실제 데이터를 배열로 한 번 더 감싼다
        // PROPERTY 방식은 타입 식별자를 클래스명만 사용한다
        // 따라서 위 정보만을 가지고 역직렬화 시 ArrayList라는 클래스 이름만으로는 타입을 알 수 없기에 아래 예외가 발생한다
        // Could not resolve type id 'ArrayList' as a subtype of `java.util.List<learn.jackson.databind.annotation.mixin.MixInInterfaceTest$Address>`:
        //  known type ids = [] (for POJO property 'addresses')
    }

    @Test
    void nameTypeIdExists() throws JsonProcessingException {
        List<Address> addresses = new ArrayList<>();
        addresses.add(new Address("주소1"));
        addresses.add(new Address("주소2"));
        Member member = new Member("아무개", addresses);

        TypeResolverBuilder<StdTypeResolverBuilder> typer =
            new DefaultTypeResolverBuilder(DefaultTyping.EVERYTHING, mapper.getPolymorphicTypeValidator());
        typer = typer.init(Id.NAME, null);
        typer = typer.inclusion(As.PROPERTY);
        mapper.setDefaultTyping(typer);

        mapper.addMixIn(List.class, ListMixIn.class);
        // List 코드를 직접 수정할 수 없으니 믹스인 인터페이스를 사용하여 List의 구현 타입을 직렬화할 수 있게 한다

        String json = mapper.writeValueAsString(member);
        assertThat(json).isEqualTo("""
                                       {
                                         "@type" : "MixInInterfaceTest$Member",
                                         "name" : "아무개",
                                         "addresses" : [ "java.util.ArrayList", [ {
                                           "@type" : "MixInInterfaceTest$Address",
                                           "name" : "주소1"
                                         }, {
                                           "@type" : "MixInInterfaceTest$Address",
                                           "name" : "주소2"
                                         } ] ]
                                       }""");

        Member deserializedMember = mapper.readValue(json, Member.class);
        assertThat(deserializedMember.getName()).isEqualTo("아무개");

        List<Address> deserializedAddresses = deserializedMember.getAddresses();
        assertThat(deserializedAddresses.get(0).getName()).isEqualTo("주소1");
        assertThat(deserializedAddresses.get(1).getName()).isEqualTo("주소2");
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class Member {

        private String name;

        private List<Address> addresses;

        public Member(String name, List<Address> addresses) {
            this.name = name;
            this.addresses = addresses;
        }
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    static class Address {

        private String name;

        public Address(String name) {
            this.name = name;
        }
    }

    @JsonTypeInfo(use = Id.CLASS)
    interface ListMixIn {

    }
}
