package com.lwx1145.sampleintegration.compat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;

import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Runtime bridge for JsonUtils signatures that differ across launcher/runtime stacks.
 */
public final class JsonUtilsCompat {

    private static final Method FROM_JSON_STRING_TYPE = resolve("fromJson", Gson.class, String.class, Type.class, boolean.class);
    private static final Method FROM_JSON_STRING_CLASS = resolve("fromJson", Gson.class, String.class, Class.class, boolean.class);
    private static final Method FROM_JSON_READER_TYPE = resolve("fromJson", Gson.class, Reader.class, Type.class, boolean.class);
    private static final Method FROM_JSON_READER_CLASS = resolve("fromJson", Gson.class, Reader.class, Class.class, boolean.class);
    private static final Method FROM_JSON_READER_CLASS_NO_LENIENT = resolve("fromJson", Gson.class, Reader.class, Class.class);
    private static final Method GET_STRING_DEFAULT = resolve("getString", JsonObject.class, String.class, String.class);
    private static final Method GET_JSON_ARRAY = resolve("getJsonArray", JsonObject.class, String.class);
    private static final Method GET_JSON_ARRAY_DEFAULT = resolve("getJsonArray", JsonObject.class, String.class, JsonArray.class);

    private JsonUtilsCompat() {
    }

    public static Object fromJson(Gson gson, String json, Type type, boolean lenient) {
        Object direct = invokeSafely(FROM_JSON_STRING_TYPE, gson, json, type, lenient);
        if (direct != null || FROM_JSON_STRING_TYPE != null) {
            return direct;
        }

        if (type instanceof Class<?>) {
            Object classBased = invokeSafely(FROM_JSON_STRING_CLASS, gson, json, type, lenient);
            if (classBased != null || FROM_JSON_STRING_CLASS != null) {
                return classBased;
            }
        }

        JsonReader reader = new JsonReader(new StringReader(json));
        reader.setLenient(lenient);
        return gson.fromJson(reader, type);
    }

    public static Object fromJson(Gson gson, Reader input, Type type, boolean lenient) {
        Object direct = invokeSafely(FROM_JSON_READER_TYPE, gson, input, type, lenient);
        if (direct != null || FROM_JSON_READER_TYPE != null) {
            return direct;
        }

        if (type instanceof Class<?>) {
            Object classBased = invokeSafely(FROM_JSON_READER_CLASS, gson, input, type, lenient);
            if (classBased != null || FROM_JSON_READER_CLASS != null) {
                return classBased;
            }
        }

        JsonReader reader = new JsonReader(input);
        reader.setLenient(lenient);
        return gson.fromJson(reader, type);
    }

    public static Object fromJson(Gson gson, Reader input, Class<?> clazz) {
        Object direct = invokeSafely(FROM_JSON_READER_CLASS_NO_LENIENT, gson, input, clazz);
        if (direct != null || FROM_JSON_READER_CLASS_NO_LENIENT != null) {
            return direct;
        }
        return gson.fromJson(input, clazz);
    }

    public static String getString(JsonObject json, String memberName, String fallback) {
        Object direct = invokeSafely(GET_STRING_DEFAULT, json, memberName, fallback);
        if (direct != null || GET_STRING_DEFAULT != null) {
            return (String) direct;
        }
        if (json == null || memberName == null || !json.has(memberName)) {
            return fallback;
        }
        JsonElement element = json.get(memberName);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (element.isJsonPrimitive()) {
            return element.getAsString();
        }
        throw new JsonSyntaxException("Expected '" + memberName + "' to be a string");
    }

    public static JsonArray getJsonArray(JsonObject json, String memberName) {
        Object direct = invokeSafely(GET_JSON_ARRAY, json, memberName);
        if (direct != null || GET_JSON_ARRAY != null) {
            return (JsonArray) direct;
        }
        if (json == null || memberName == null || !json.has(memberName)) {
            throw new JsonSyntaxException("Missing '" + memberName + "', expected JsonArray");
        }
        JsonElement element = json.get(memberName);
        if (element != null && element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        throw new JsonSyntaxException("Expected '" + memberName + "' to be JsonArray");
    }

    public static JsonArray getJsonArray(JsonObject json, String memberName, JsonArray fallback) {
        Object direct = invokeSafely(GET_JSON_ARRAY_DEFAULT, json, memberName, fallback);
        if (direct != null || GET_JSON_ARRAY_DEFAULT != null) {
            return (JsonArray) direct;
        }
        if (json == null || memberName == null || !json.has(memberName)) {
            return fallback;
        }
        JsonElement element = json.get(memberName);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (element.isJsonArray()) {
            return element.getAsJsonArray();
        }
        throw new JsonSyntaxException("Expected '" + memberName + "' to be JsonArray");
    }

    private static Method resolve(String name, Class<?>... params) {
        try {
            Class<?> jsonUtils = Class.forName("net.minecraft.util.JsonUtils");
            Method method = jsonUtils.getDeclaredMethod(name, params);
            method.setAccessible(true);
            return method;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object invokeSafely(Method method, Object gson, Object input, Object type, boolean lenient) {
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(null, gson, input, type, lenient);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Object invokeSafely(Method method, Object arg0, Object arg1, Object arg2) {
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(null, arg0, arg1, arg2);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private static Object invokeSafely(Method method, Object arg0, Object arg1) {
        if (method == null) {
            return null;
        }
        try {
            return method.invoke(null, arg0, arg1);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
