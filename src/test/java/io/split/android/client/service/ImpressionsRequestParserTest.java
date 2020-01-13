package io.split.android.client.service;

import com.google.gson.reflect.TypeToken;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.utils.Json;

public class ImpressionsRequestParserTest {

    static final private Type IMPRESSIONS_BODY_REQUEST_TYPE
            = new TypeToken<List<TestImpressions>>() {
    }.getType();

    HttpRequestBodySerializer<List<KeyImpression>> parser = new ImpressionsRequestBodySerializer();

    @Test
    public void parsing() {
        List<KeyImpression> impressions = createImpressions(1, 5, "feature_1");
        impressions.addAll(createImpressions(1, 5, "feature_2"));
        impressions.addAll(createImpressions(11, 15, "feature_2"));
        impressions.addAll(createImpressions(201, 215, "feature_3"));

        String jsonTestImpressions = parser.serialize(impressions);
        List<TestImpressions> testImpressions = Json.fromJson(jsonTestImpressions, IMPRESSIONS_BODY_REQUEST_TYPE);

        List<KeyImpression> feat1Imp = impressionsForTest("feature_1", testImpressions);
        List<KeyImpression> feat2Imp = impressionsForTest("feature_2", testImpressions);
        List<KeyImpression> feat3Imp = impressionsForTest("feature_3", testImpressions);

        Assert.assertEquals(3, testImpressions.size());
        Assert.assertEquals(5, feat1Imp.size());
        Assert.assertEquals(10, feat2Imp.size());
        Assert.assertEquals(15, feat3Imp.size());
    }

    private List<KeyImpression> createImpressions(int from, int to, String feature) {
        List<KeyImpression> impressions = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            KeyImpression impression = new KeyImpression();
            impression.keyName = "Impression_" + i;
            impression.feature = feature;
            impression.time = 11111;
            impression.changeNumber = 9999L;
            impression.label = "default rule";
            impressions.add(impression);
        }
        return impressions;
    }

    private List<KeyImpression> impressionsForTest(String testName, List<TestImpressions> testImpressions) {
        List<KeyImpression> impressions = new ArrayList<>();
        for (TestImpressions test : testImpressions) {
            if (test.testName.equals(testName)) {
                impressions.addAll(test.keyImpressions);
            }
        }
        return impressions;
    }
}
