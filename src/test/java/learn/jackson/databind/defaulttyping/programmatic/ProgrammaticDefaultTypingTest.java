package learn.jackson.databind.defaulttyping.programmatic;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTypeResolverBuilder;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.TypeResolverBuilder;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;

class ProgrammaticDefaultTypingTest {

    @Test
    void withoutDefaultTyping() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        User user = new User("Smith");

        assertThat(mapper.writeValueAsString(user)).isEqualTo("""
                                                                  {
                                                                    "name" : "Smith"
                                                                  }""");
    }

    @Test
    void polymorphicTypeValidator() throws JsonProcessingException {
        ObjectMapper mapper1 = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .activateDefaultTyping(BasicPolymorphicTypeValidator.builder().build(),
                                   DefaultTyping.NON_FINAL,
                                   As.WRAPPER_ARRAY); // includeAs의 기본 값: WRAPPER_ARRAY

        User user = new User("Smith");

        assertThat(mapper1.writeValueAsString(user))
            .isEqualTo("""
                           [ "learn.jackson.databind.defaulttyping.programmatic.ProgrammaticDefaultTypingTest$User", {
                             "name" : "Smith"
                           } ]""");

        ObjectMapper mapper2 = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT)
            .activateDefaultTyping(BasicPolymorphicTypeValidator.builder().build(),
                                   DefaultTyping.NON_FINAL,
                                   As.PROPERTY);
        assertThat(mapper2.writeValueAsString(user))
            .isEqualTo("""
                           {
                             "@class" : "learn.jackson.databind.defaulttyping.programmatic.ProgrammaticDefaultTypingTest$User",
                             "name" : "Smith"
                           }""");
    }

    @Test
    void typeResolverBuilder() throws JsonProcessingException {
        User user = new User("Smith");

        ObjectMapper mapper1 = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        mapper1.setDefaultTyping(DefaultTypeResolverBuilder.noTypeInfoBuilder());

        assertThat(mapper1.writeValueAsString(user)).isEqualTo("""
                                                                   {
                                                                     "name" : "Smith"
                                                                   }""");

        // ~ Spring Data Redis의 GenericJackson2JsonRedisSerializer의 로직
        ObjectMapper mapper2 = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        TypeResolverBuilder<StdTypeResolverBuilder> typer =
            new DefaultTypeResolverBuilder(DefaultTyping.EVERYTHING,
                                           mapper2.getPolymorphicTypeValidator());
        typer = typer.init(Id.CLASS, null);
        typer = typer.inclusion(As.PROPERTY);
        // typer = typer.typeProperty(?); // @JsonTypeInfo의 property 엘리먼트와 동일한 기능

        mapper2.setDefaultTyping(typer);

        assertThat(mapper2.writeValueAsString(user))
            .isEqualTo("""
                           {
                             "@class" : "learn.jackson.databind.defaulttyping.programmatic.ProgrammaticDefaultTypingTest$User",
                             "name" : "Smith"
                           }""");
    }

    @RequiredArgsConstructor
    @Getter
    static class User {

        private final String name;
    }
}
