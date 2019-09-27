package io.split.android;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Split;
import io.split.android.client.utils.Json;

public class SplitConfigurationsParsingTests {

    @Test
    public void testEncodingOneBasicConfig() {
        String config = "{ \"treatment1\": \"{\\\"c1\\\": \\\"v1\\\"}\"}";
        Split split = createAndParseSplit(config);
        Map<String, Object> configData = jsonObj(split.configurations.get("treatment1"));
        Assert.assertNotNull(split);
        Assert.assertEquals("v1", (String) configData.get("c1"));
    }

    @Test
    public void encodingBasicArrayConfig() {

        String config = "{ \"treatment1\": \"{\\\"c1\\\": [1, 2.0, 3, 4.0]}\"}";
        Split split = createAndParseSplit(config);
        Map<String, Object> configData = jsonObj(split.configurations.get("treatment1"));
        List<Double> array = (List<Double>) configData.get("c1");
        Assert.assertNotNull(split);
        Assert.assertEquals(4, array.size());
        Assert.assertEquals(Double.valueOf(1), (Double) array.get(0));
        Assert.assertEquals(Double.valueOf(2.0), (Double) array.get(1));
        Assert.assertEquals(Double.valueOf(3), (Double) array.get(2));
        Assert.assertEquals(Double.valueOf(4.0), (Double) array.get(3));
    }

    @Test
    public void testEncodingMapArrayConfig() {

        String config = "{\"treatment1\": \"{\\\"a1\\\":[{\\\"f\\\":\\\"v1\\\"}, {\\\"f\\\":\\\"v2\\\"}, {\\\"f\\\":\\\"v3\\\"}], \\\"a2\\\":[{\\\"f1\\\":1, \\\"f2\\\":2, \\\"f3\\\":3}, {\\\"f1\\\":11, \\\"f2\\\": 12, \\\"f3\\\":13}]}\", \"treatment2\": \"{\\\"f1\\\":\\\"v1\\\"}\"}";
        Split split = createAndParseSplit(config);
        Map<String, Object> config1Data = jsonObj(split.configurations.get("treatment1"));
        Map<String, Object> config2Data = jsonObj(split.configurations.get("treatment2"));
        List<Object> array1 = (List<Object>) config1Data.get("a1");
        List<Object> array2 = (List<Object>) config1Data.get("a2");
        Map<String, String> obj11 = (Map<String, String>) array1.get(0);
        Map<String, String> obj12 = (Map<String, String>) array1.get(1);
        Map<String, String> obj13 = (Map<String, String>) array1.get(2);
        Map<String, Double> obj21 = (Map<String, Double>) array2.get(0);
        Map<String, Double> obj22 = (Map<String, Double>) array2.get(1);


        Assert.assertNotNull(split);
        Assert.assertNotNull(config1Data);
        Assert.assertNotNull(config2Data);
        Assert.assertNotNull(array1);
        Assert.assertNotNull(array2);
        Assert.assertEquals(3, array1.size());
        Assert.assertEquals(2, array2.size());

        Assert.assertEquals("v1", obj11.get("f"));
        Assert.assertEquals("v2", obj12.get("f"));
        Assert.assertEquals("v3", obj13.get("f"));

        Assert.assertEquals(Double.valueOf(1), (Double) obj21.get("f1"));
        Assert.assertEquals(Double.valueOf(2), (Double) obj21.get("f2"));
        Assert.assertEquals(Double.valueOf(3), (Double) obj21.get("f3"));
        Assert.assertEquals(Double.valueOf(11), (Double) obj22.get("f1"));
        Assert.assertEquals(Double.valueOf(12), (Double) obj22.get("f2"));
        Assert.assertEquals(Double.valueOf(13), (Double) obj22.get("f3"));

        Assert.assertEquals("v1", config2Data.get("f1"));
    }

    @Test
    public void testEncodingMultiTreatmentConfig() {

        String config = "{ \"treatment1\": \"{\\\"c1\\\": \\\"v1\\\"}\", \"treatment2\": \"{\\\"c1\\\": \\\"v1\\\"}\", \"treatment3\": \"{\\\"c1\\\": \\\"v1\\\"}\"}";
        Split split = createAndParseSplit(config);
        Map<String, Object> config1 = jsonObj(split.configurations.get("treatment1"));
        Map<String, Object> config2 = jsonObj(split.configurations.get("treatment2"));
        Map<String, Object> config3 = jsonObj(split.configurations.get("treatment3"));

        Assert.assertNotNull(split);
        Assert.assertEquals("v1", config1.get("c1"));
        Assert.assertEquals("v1", config2.get("c1"));
        Assert.assertEquals("v1", config3.get("c1"));
    }

    @Test
    public void testEncodingAllValueTypesConfig() {

        String config = "{ \"double\": \"{\\\"c1\\\": 20576.85}\", \"string\": \"{\\\"c1\\\": \\\"v1\\\"}\", \"int\": \"{\\\"c1\\\": 123456}\", \"boolean\": \"{\\\"c1\\\": false}\"}";
        Split split = createAndParseSplit(config);
        Map<String, Object> doubleValue = jsonObj(split.configurations.get("double"));
        Map<String, Object> stringValue = jsonObj(split.configurations.get("string"));
        Map<String, Object> intValue = jsonObj(split.configurations.get("int"));
        Map<String, Object> booleanValue = jsonObj(split.configurations.get("boolean"));

        Assert.assertNotNull(split);
        Assert.assertEquals(Double.valueOf(20576.85), (Double) doubleValue.get("c1"));
        Assert.assertEquals("v1", stringValue.get("c1"));
        Assert.assertEquals(123456, ((Double) intValue.get("c1")).intValue());
        Assert.assertEquals(false, booleanValue.get("c1"));
    }

    @Test
    public void testEncodingNestedMultiConfig() {

        String config = "{\"treatment1\": \"{\\\"f1\\\": 10,\\\"f2\\\":\\\"v2\\\",\\\"nested1\\\":{\\\"nv1\\\":\\\"nval1\\\"}, \\\"nested2\\\":{\\\"nv2\\\":\\\"nval2\\\"}}\" , \"treatment2\": \"{\\\"f1\\\": 10.20,\\\"f2\\\":true,\\\"nested3\\\":{\\\"nested4\\\":{\\\"nv2\\\": \\\"nval3\\\"}}}\"}";
        Split split = createAndParseSplit(config);
        Map<String, Object> config1 = jsonObj(split.configurations.get("treatment1"));
        Map<String, Object> config2 = jsonObj(split.configurations.get("treatment2"));
        Map<String, Object> nested1 = (Map<String, Object>) config1.get("nested1");
        Map<String, Object> nested2 = (Map<String, Object>) config1.get("nested2");
        Map<String, Object> nested3 = (Map<String, Object>) config2.get("nested3");
        Map<String, Object> nested4 = (Map<String, Object>) nested3.get("nested4");

        Assert.assertNotNull(split);
        Assert.assertEquals(Double.valueOf(10), (Double) config1.get("f1"));
        Assert.assertEquals("v2", config1.get("f2"));

        Assert.assertNotNull(nested1);
        Assert.assertNotNull(nested2);
        Assert.assertNotNull(nested3);
        Assert.assertNotNull(nested4);

        Assert.assertEquals("nval1", nested1.get("nv1"));
        Assert.assertEquals("nval1", nested1.get("nv1"));
        Assert.assertEquals("nval2", nested2.get("nv2"));
        Assert.assertEquals("nval3", nested4.get("nv2"));

        Assert.assertEquals(Double.valueOf(10.20), (Double) config2.get("f1"));
        Assert.assertEquals(true, config2.get("f2"));
    }

    @Test
    public void testEncodingNullConfig() {
        Split split = createAndParseSplit(null);

        Assert.assertNotNull(split);
        Assert.assertNull(split.configurations);
    }


    @Test
    public void testDecodingSimpleConfig() {
        String config = "{ \"treatment1\": \"{\\\"c1\\\": \\\"v1\\\"}\", \"treatment2\": \"{\\\"c1\\\": \\\"v1\\\"}\"}";
        Split initialSplit = createAndParseSplit(config);
        String jsonSplit = Json.toJson(initialSplit);
        Split split = null;
        if (jsonSplit != null) {
            split = Json.fromJson(jsonSplit, Split.class);
        }
        Map<String, Object> t1Config = (Map<String, Object>) jsonObj(split.configurations.get("treatment1"));
        Map<String, Object> t2Config = (Map<String, Object>) jsonObj(split.configurations.get("treatment2"));

        Assert.assertNotNull(split);
        Assert.assertNotNull(split.configurations);
        Assert.assertEquals("v1", t1Config.get("c1"));
        Assert.assertEquals("v1", t2Config.get("c1"));
    }

    @Test
    public void testDecodingArrayAndMapConfig() {
        String config = "{ \"treatment1\": \"{\\\"c1\\\": \\\"v1\\\"}\", \"treatment2\": \"{\\\"a1\\\": [1,2,3,4], \\\"m1\\\": {\\\"c1\\\": \\\"v1\\\"}}\"}";
        Split initialSplit = createAndParseSplit(config);
        String jsonSplit = Json.toJson(initialSplit);
        Split split = null;
        if (jsonSplit != null) {
            split = Json.fromJson(jsonSplit, Split.class);
        }
        Map<String, Object> t1Config = jsonObj(split.configurations.get("treatment1"));
        Map<String, Object> t2Config = jsonObj(split.configurations.get("treatment2"));
        List<Double> array = (List<Double>) t2Config.get("a1");
        Map<String, String> map = (Map<String, String>) t2Config.get("m1");

        Assert.assertNotNull(split);
        Assert.assertNotNull(split.configurations);
        Assert.assertNotNull(t1Config);
        Assert.assertNotNull(t2Config);
        Assert.assertNotNull(array);
        Assert.assertNotNull(map);
        Assert.assertEquals("v1", t1Config.get("c1"));
        Assert.assertEquals(4, array.size());
        Assert.assertEquals(Double.valueOf(1), (Double) array.get(0));
        Assert.assertEquals(Double.valueOf(4), (Double) array.get(3));
        Assert.assertEquals("v1", map.get("c1"));
    }

    private Split createAndParseSplit(String config) {

        String jsonSplit = "\"name\":\"TEST_FEATURE\"";
        if (config != null) {
            jsonSplit = jsonSplit + ", \"configurations\": " + config;
        }
        jsonSplit = "{" + jsonSplit + "}";
        return Json.fromJson(jsonSplit, Split.class);
    }

    private Map<String, Object> jsonObj(String json) {

        return fromJsonMap(json);
    }

    private Map<String, Object> fromJsonMap(String json) {
        try {
            Type mapType = new TypeToken<Map<String, Object>>() {
            }.getType();
            return new Gson().fromJson(json, mapType);
        } catch (Exception e) {
        }
        return null;
    }

}

