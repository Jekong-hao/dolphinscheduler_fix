/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.common.utils;

import static java.nio.charset.StandardCharsets.UTF_8;

import static com.fasterxml.jackson.databind.DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT;
import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL;
import static com.fasterxml.jackson.databind.MapperFeature.REQUIRE_SETTERS_FOR_GETTERS;

import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.fasterxml.jackson.databind.type.CollectionType;

/**
 * json utils
 */
public class JSONUtils {

    private static final Logger logger = LoggerFactory.getLogger(JSONUtils.class);

    /**
     * 单位缩进字符串。
     */
    private static final String SPACE = "   ";

    /**
     * can use static singleton, inject: just make sure to reuse!
     */
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true)
            .configure(READ_UNKNOWN_ENUM_VALUES_AS_NULL, true)
            .configure(REQUIRE_SETTERS_FOR_GETTERS, true)
            .setTimeZone(TimeZone.getDefault());

    private JSONUtils() {
        throw new UnsupportedOperationException("Construct JSONUtils");
    }

    public static ArrayNode createArrayNode() {
        return objectMapper.createArrayNode();
    }

    public static ObjectNode createObjectNode() {
        return objectMapper.createObjectNode();
    }

    public static JsonNode toJsonNode(Object obj) {
        return objectMapper.valueToTree(obj);
    }

    /**
     * 返回格式化JSON字符串。
     *
     * @param jsonString 未格式化的JSON字符串。
     * @return 格式化的JSON字符串。
     */
    public static String formatJson(String jsonString) {
        StringBuffer result = new StringBuffer();

        int length = jsonString.length();
        int number = 0;
        char key = 0;
        //遍历输入字符串。
        for (int i = 0; i < length; i++) {
            //1、获取当前字符。
            key = jsonString.charAt(i);

            //2、如果当前字符为 ’“‘,退出本次循环
            if (key == '"' || key == '[' || key == ']') {
                continue;
            }

            //3、如果当前字符是前方括号、前花括号做如下处理：
            if(key == '{') {
                //（1）如果前面还有字符，并且字符为“：”，打印：换行和缩进字符字符串。
                if((i - 1 > 0) && (jsonString.charAt(i - 1) == ':')) {
                    result.append('\n');
                    result.append(indent(number));
                }

                //（2）打印：当前字符。
                // result.append(key);
                result.append("--------------------------------");

                //（3）前方括号、前花括号，的后面必须换行。打印：换行。
                result.append('\n');

                //（4）每出现一次前方括号、前花括号；缩进次数增加一次。打印：新行缩进。
                number++;
                // result.append(indent(number));

                //（5）进行下一次循环。
                continue;
            }

            //4、如果当前字符是后方括号、后花括号做如下处理：
            if(key == '}') {
                //（1）后方括号、后花括号，的前面必须换行。打印：换行。
                result.append('\n');

                //（2）每出现一次后方括号、后花括号；缩进次数减少一次。打印：缩进。
                number--;
                // result.append(indent(number));

                //（3）打印：当前字符。
                // result.append(key);
                result.append("--------------------------------");

                //（4）如果当前字符后面还有字符，并且字符不为“，”，打印：换行。
                if(((i + 1) < length) && (jsonString.charAt(i + 1) != ',' ) && (jsonString.charAt(i + 1) != ']' )) {
                    result.append('\n');
                }

                //（5）继续下一次循环。
                continue;
            }

            if((key == ',') && (jsonString.charAt(i - 1) == '}')) {
                result.append('\n');
                continue;
            }

            //5、如果当前字符是逗号。逗号后面换行，并缩进，不改变缩进次数。
            if((key == ',')) {
                result.append(key);
                result.append('\n');
                // result.append(indent(number));
                continue;
            }

            //6、打印：当前字符。
            result.append(key);
        }

        return result.toString();
    }

    /**
     * 返回指定次数的缩进字符串。每一次缩进三个空格，即SPACE。
     *
     * @param number 缩进次数。
     * @return 指定缩进次数的字符串。
     */
    private static String indent(int number) {
        StringBuffer result = new StringBuffer();
        for(int i = 0; i < number; i++) {
            result.append(SPACE);
        }
        return result.toString();
    }

    /**
     * json representation of object
     *
     * @param object object
     * @param feature feature
     * @return object to json string
     */
    public static String toJsonString(Object object, SerializationFeature feature) {
        try {
            ObjectWriter writer = objectMapper.writer(feature);
            return writer.writeValueAsString(object);
        } catch (Exception e) {
            logger.error("object to json exception!", e);
        }

        return null;
    }

    /**
     * This method deserializes the specified Json into an object of the specified class. It is not
     * suitable to use if the specified class is a generic type since it will not have the generic
     * type information because of the Type Erasure feature of Java. Therefore, this method should not
     * be used if the desired type is a generic type. Note that this method works fine if the any of
     * the fields of the specified object are generics, just the object itself should not be a
     * generic type.
     *
     * @param json the string from which the object is to be deserialized
     * @param clazz the class of T
     * @param <T> T
     * @return an object of type T from the string
     * classOfT
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }
//        System.out.println(json);
//        System.out.println(clazz);
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            logger.error("parse object exception!", e);
        }
        return null;
    }

    /**
     *  deserialize
     *
     * @param src byte array
     * @param clazz class
     * @param <T> deserialize type
     * @return deserialize type
     */
    public static <T> T parseObject(byte[] src, Class<T> clazz) {
        if (src == null) {
            return null;
        }
        String json = new String(src, UTF_8);
        return parseObject(json, clazz);
    }

    /**
     * json to list
     *
     * @param json json string
     * @param clazz class
     * @param <T> T
     * @return list
     */
    public static <T> List<T> toList(String json, Class<T> clazz) {
        if (StringUtils.isEmpty(json)) {
            return Collections.emptyList();
        }

        try {
            CollectionType listType = objectMapper.getTypeFactory().constructCollectionType(ArrayList.class, clazz);
            return objectMapper.readValue(json, listType);
        } catch (Exception e) {
            logger.error("parse list exception!", e);
        }

        return Collections.emptyList();
    }

    /**
     * check json object valid
     *
     * @param json json
     * @return true if valid
     */
    public static boolean checkJsonValid(String json) {

        if (StringUtils.isEmpty(json)) {
            return false;
        }

        try {
            objectMapper.readTree(json);
            return true;
        } catch (IOException e) {
            logger.error("check json object valid exception!", e);
        }

        return false;
    }

    /**
     * Method for finding a JSON Object field with specified name in this
     * node or its child nodes, and returning value it has.
     * If no matching field is found in this node or its descendants, returns null.
     *
     * @param jsonNode json node
     * @param fieldName Name of field to look for
     * @return Value of first matching node found, if any; null if none
     */
    public static String findValue(JsonNode jsonNode, String fieldName) {
        JsonNode node = jsonNode.findValue(fieldName);

        if (node == null) {
            return null;
        }

        return node.asText();
    }

    /**
     * json to map
     * {@link #toMap(String, Class, Class)}
     *
     * @param json json
     * @return json to map
     */
    public static Map<String, String> toMap(String json) {
        return parseObject(json, new TypeReference<Map<String, String>>() {});
    }

    /**
     * from the key-value generated json  to get the str value no matter the real type of value
     * @param json the json str
     * @param nodeName key
     * @return the str value of key
     */
    public static String getNodeString(String json, String nodeName) {
        try {
            JsonNode rootNode = objectMapper.readTree(json);
            return rootNode.has(nodeName) ? rootNode.get(nodeName).toString() : "";
        } catch (JsonProcessingException e) {
            return "";
        }
    }

    /**
     * json to object
     *
     * @param json json string
     * @param type type reference
     * @param <T>
     * @return return parse object
     */
    public static <T> T parseObject(String json, TypeReference<T> type) {
        if (StringUtils.isEmpty(json)) {
            return null;
        }

        try {
            return objectMapper.readValue(json, type);
        } catch (Exception e) {
            logger.error("json to map exception!", e);
        }

        return null;
    }

    /**
     * object to json string
     *
     * @param object object
     * @return json string
     */
    public static String toJsonString(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException("Object json deserialization exception.", e);
        }
    }

    /**
     * serialize to json byte
     *
     * @param obj object
     * @param <T> object type
     * @return byte array
     */
    public static <T> byte[] toJsonByteArray(T obj)  {
        if (obj == null) {
            return null;
        }
        String json = "";
        try {
            json = toJsonString(obj);
        } catch (Exception e) {
            logger.error("json serialize exception.", e);
        }

        return json.getBytes(UTF_8);
    }

    public static ObjectNode parseObject(String text) {
        try {
            if (text.isEmpty()) {
                return parseObject(text, ObjectNode.class);
            } else {
                return (ObjectNode) objectMapper.readTree(text);
            }
        } catch (Exception e) {
            throw new RuntimeException("String json deserialization exception.", e);
        }
    }

    public static ArrayNode parseArray(String text) {
        try {
            return (ArrayNode) objectMapper.readTree(text);
        } catch (Exception e) {
            throw new RuntimeException("Json deserialization exception.", e);
        }
    }

    /**
     * json serializer
     */
    public static class JsonDataSerializer extends JsonSerializer<String> {

        @Override
        public void serialize(String value, JsonGenerator gen, SerializerProvider provider) throws IOException {
            gen.writeRawValue(value);
        }

    }

    /**
     * json data deserializer
     */
    public static class JsonDataDeserializer extends JsonDeserializer<String> {

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            JsonNode node = p.getCodec().readTree(p);
            if (node instanceof TextNode) {
                return node.asText();
            } else {
                return node.toString();
            }
        }

    }
}
