package com.acme;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.minidev.json.JSONArray;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static java.util.Objects.*;

/**
 * Build JSON documents from JSON Path entries
 */
public final class JsonBuilder {

    public static final String PATH_ROOT = "$.";

    private static final ObjectMapper DEFAULT_MAPPER = new ObjectMapper();

    private static final String EMPTY_JSON = "{}";

    private final DocumentContext body;

    private ObjectMapper jsonMapper;

    private JsonBuilder() {
        body = JsonPath.parse(EMPTY_JSON);
    }

    private JsonBuilder(ObjectMapper jsonMapper) {
        this();
        this.jsonMapper = jsonMapper;
    }

    /**
     * JsonBuilder factory
     *
     * @return JsonNode
     */
    public static JsonBuilder builder() {
        return new JsonBuilder();
    }

    /**
     * JsonBuilder factory
     *
     * @param jsonMapper Custom mapper
     * @return JsonNode
     */
    public static JsonBuilder builder(ObjectMapper jsonMapper) {
        requireNonNull(jsonMapper);
        return new JsonBuilder(jsonMapper);
    }

    /**
     * Build empty JsonNode
     *
     * @return JsonNode
     */
    public static JsonNode buildEmpty() throws JsonProcessingException {
        return DEFAULT_MAPPER.readTree(EMPTY_JSON);
    }

    /**
     * Build JsonNode from informed entries
     *
     * @return JsonNode
     * @throws JsonProcessingException
     */
    public JsonNode build() throws JsonProcessingException {
        final ObjectMapper mapper = nonNull(jsonMapper) ? jsonMapper : DEFAULT_MAPPER;
        return mapper.readTree(body.jsonString());
    }

    /**
     * Build JsonNode from informed entries then turn it into Map
     *
     * @return JsonNode
     * @throws JsonProcessingException
     */
    public Map<?, ?> buildAsMap() throws JsonProcessingException {
        final ObjectMapper mapper = nonNull(jsonMapper) ? jsonMapper : DEFAULT_MAPPER;
        JsonNode node = mapper.readTree(body.jsonString());
        return mapper.treeToValue(node, Map.class);
    }

    /**
     * Silent build JsonNode from informed entries, ignoring any exception
     *
     * @return JsonNode
     */
    public JsonNode silentBuild() {
        try {
            return build();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return NullNode.getInstance();
        }
    }

    /**
     * Silent build JsonNode from informed entries then turn it into Map, ignoring any exception
     *
     * @return Map
     */
    public Map<?, ?> silentBuildAsMap() {
        try {
            return buildAsMap();
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

    /**
     * Put entry from supplier function
     *
     * @param path     JSONPath
     * @param supplier Entry supplier
     * @return JsonBuilder
     * @throws JsonProcessingException
     */
    public JsonBuilder put(String path, Supplier<Object> supplier) throws JsonProcessingException {
        return put(path, supplier.get());
    }

    /**
     * Put entry
     *
     * @param path  JSONPath
     * @param value Entry value
     * @return JsonBuilder
     * @throws JsonProcessingException
     */
    public JsonBuilder put(String path, Object value) throws JsonProcessingException {
        final ObjectMapper mapper = nonNull(jsonMapper) ? jsonMapper : DEFAULT_MAPPER;

        // Replace null values by null nodes
        if (isNull(value)) {
            value = NullNode.getInstance();
        }

        if (path.startsWith(PATH_ROOT)) {
            path = path.replace(PATH_ROOT, "");
        }

        String[] levels = path.split("\\.");
        String acc = "$";
        // Put parents
        for (int i = 0; i < levels.length - 1; ++i) {
            try {
                body.read(acc + "." + levels[i]);
            } catch (PathNotFoundException ex) {
                if (levels[i].contains("[")) {
                    body.put(acc, levels[i], new JSONArray());
                } else {
                    body.put(acc, levels[i], new HashMap<String, Object>());
                }
            }
            acc = acc + "." + levels[i];
        }

        // Put leaf
        if (levels[levels.length - 1].contains("[")) {
            String arrayPath = levels[levels.length - 1].replace("[*]", "");
            try {
                body.read(acc + "." + arrayPath);
            } catch (PathNotFoundException ex) {
                body.put(acc, arrayPath, new JSONArray());
            }
            if (value.toString().contains("{")) {
                JsonNode valueNode = mapper.readTree(value.toString());
                body.add(acc + "." + arrayPath, mapper.treeToValue(valueNode, Map.class));
            } else {
                body.add(acc + "." + arrayPath, value);
            }
        } else {
            if (value instanceof Map) {
                // Support "put" from "buildAsMap"
                String json = mapper.writeValueAsString(value);
                JsonNode valueNode = mapper.readTree(json);
                body.put(acc, levels[levels.length - 1], mapper.treeToValue(valueNode, Map.class));
            } else if (value.toString().contains("{")) {
                JsonNode valueNode = mapper.readTree(value.toString());
                body.put(acc, levels[levels.length - 1], mapper.treeToValue(valueNode, Map.class));
            } else {
                body.put(acc, levels[levels.length - 1], value);
            }
        }

        return this;
    }


    /**
     * Silent put entry, ignoring any exception
     *
     * @param path  JSONPath
     * @param value Entry value
     * @return JsonBuilder
     */
    public JsonBuilder silentPut(String path, Object value) {
        try {
            return put(path, value);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return this;
        }
    }

    /**
     * Silent put from supplier function, ignoring any exception
     *
     * @param path     JSONPath
     * @param supplier Entry supplier
     * @return JsonBuilder
     */
    public JsonBuilder silentPut(String path, Supplier<Object> supplier) {
        try {
            return put(path, supplier.get());
        } catch (JsonProcessingException e) {
            return this;
        }
    }

    /**
     * Delete entry
     *
     * @param path JSONPath
     * @return JsonBuilder
     */
    public JsonBuilder delete(String path) {
        if (!path.startsWith(PATH_ROOT)) {
            path = PATH_ROOT + path;
        }
        body.delete(path);
        return this;
    }

}
