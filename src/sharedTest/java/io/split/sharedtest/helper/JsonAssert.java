package io.split.sharedtest.helper;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;

public class JsonAssert {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void assertJsonEquals(String expectedJson, String actualJson) {
        JsonElement expected = JsonParser.parseString(expectedJson);
        JsonElement actual = JsonParser.parseString(actualJson);

        if (!expected.equals(actual)) {
            String prettyExpected = GSON.toJson(expected);
            String prettyActual = GSON.toJson(actual);
            Assert.assertEquals(prettyExpected, prettyActual);
        }
    }
}
