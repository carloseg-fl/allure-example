package com.acme;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class JsonBuilderTest {

    @Nested
    @DisplayName("Build")
    class Build {

        @Test
        @DisplayName("Given a map of JSONPath, should build a JSON document")
        void build() throws Exception {
            // Test
            JsonNode node = JsonBuilder.builder()
              .put("$.user.firstName", "John")
              .put("$.user.lastName", "Doe")
              .put("$.user.friends[*]", "Marco")
              .put("$.user.friends[*]", "Polo")
              .put("$.user.details.social[*]", "{\"facebook\": \"url\"}")
              .build();

            JsonNode userNode = node.get("user");
            assertThat(userNode.get("firstName").asText(), equalTo("John"));
            assertThat(userNode.get("lastName").asText(), equalTo("Doe"));
            assertThat(userNode.get("friends").get(0).asText(), equalTo("Marco"));
            assertThat(userNode.get("friends").get(1).asText(), equalTo("Polo"));
            assertThat(userNode.get("details").get("social").get(0).get("facebook").asText(), equalTo("url"));
        }

        @Test
        @DisplayName("Given a map of JSONPath, should build a Map")
        void buildAsMap() throws Exception {
            // Test
            Map<?, ?> map = JsonBuilder.builder()
              .put("$.user.firstName", "John")
              .put("$.user.lastName", "Doe")
              .put("$.user.friends[*]", "Marco")
              .put("$.user.friends[*]", "Polo")
              .put("$.user.details.social[*]", "{\"facebook\": \"url\"}")
              .buildAsMap();

            Map<?, ?> userMap = (Map<?, ?>) map.get("user");
            assertThat(userMap.get("firstName"), equalTo("John"));
            assertThat(userMap.get("lastName"), equalTo("Doe"));
            assertThat(((List<?>) userMap.get("friends")).get(0), equalTo("Marco"));
            assertThat(((List<?>) userMap.get("friends")).get(1), equalTo("Polo"));
        }

        @Test
        @DisplayName("Given custom JSON mapper, should use it")
        void customMapper() throws Exception {
            // Mocks
            ObjectMapper mapperMock = mock(ObjectMapper.class);
            when(mapperMock.readTree(anyString()))
              .thenReturn(MissingNode.getInstance());

            // Test
            JsonBuilder.builder()
              .put("$.user.firstName", "John")
              .build();

            // Check mock
            verify(mapperMock, never())
              .readTree(anyString());

            // Test
            JsonBuilder.builder(mapperMock)
              .put("$.user.firstName", "John")
              .build();

            // Check mock
            verify(mapperMock, times(1))
              .readTree(anyString());
        }

        @Test
        @DisplayName("Given deleted entries on JSON Path map, should reflect JSON document")
        public void delete() throws Exception {
            // Test
            JsonBuilder builder = JsonBuilder.builder()
              .put("$.user.firstName", "John")
              .put("$.user.lastName", "Doe");

            JsonNode node = builder.build();
            assertThat(node.get("user").has("firstName"), is(true));
            assertThat(node.get("user").has("lastName"), is(true));

            builder.delete("$.user.firstName");

            node = builder.build();
            assertThat(node.get("user").has("firstName"), is(false));
            assertThat(node.get("user").has("lastName"), is(true));
        }

        @Test
        @DisplayName("Should build empty JSON")
        public void empty() throws Exception {
            // Test
            JsonNode node = JsonBuilder.buildEmpty();
            assertThat(node.isEmpty(), is(true));
        }
    }

    @Nested
    @DisplayName("JSONPath")
    class Path {

        @Test
        @DisplayName("Given put with missing root '$', should add it")
        void build() throws Exception {
            // Test
            JsonNode node = JsonBuilder.builder()
              .put("$.user.firstName", "John")
              .put("user.lastName", "Doe") // missing $
              .build();

            JsonNode userNode = node.get("user");
            assertThat(userNode.get("firstName").asText(), equalTo("John"));
            assertThat(userNode.get("lastName").asText(), equalTo("Doe"));
        }

        @Test
        @DisplayName("Given delete with missing root '$', should add it")
        public void delete() throws Exception {
            // Test
            JsonBuilder builder = JsonBuilder.builder()
              .put("$.user.firstName", "John")
              .put("$.user.lastName", "Doe");

            builder.build();

            builder.delete("user.firstName"); // missing $

            JsonNode node = builder.build();
            assertThat(node.get("user").has("firstName"), is(false));
            assertThat(node.get("user").has("lastName"), is(true));
        }
    }

    @Nested
    @DisplayName("Put")
    class Put {

        @Test
        @DisplayName("Given put passing a map, should add it")
        void putMap() throws Exception {
            // Test
            JsonNode node = JsonBuilder.builder()
              .put("$.nested",  JsonBuilder.builder()
              .put("$.user.firstName", "John")
              .put("$.user.lastName", "Doe")
              .buildAsMap()
            ).build();

            JsonNode userNode = node.get("nested").get("user");
            assertThat(userNode.get("firstName").asText(), equalTo("John"));
            assertThat(userNode.get("lastName").asText(), equalTo("Doe"));
        }

    }
}
