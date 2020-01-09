package io.split.android.client.service.impressions;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.service.HttpRequestBodySerializer;
import io.split.android.client.utils.Json;

public class ImpressionsRequestBodySerializer implements HttpRequestBodySerializer<List<KeyImpression>> {

    public String serialize(@NonNull List<KeyImpression> data) {
        return Json.toJson(groupImpressions(data));
    }

    private List<TestImpressions> groupImpressions(List<KeyImpression> impressions) {

        Map<String, List<KeyImpression>> groupingImpressions = new HashMap<>();
        for(KeyImpression impression : impressions) {
            List<KeyImpression> featureImpressions = groupingImpressions.get(impression.feature);
            if(featureImpressions == null) {
                featureImpressions = new ArrayList<>();
            }
            featureImpressions.add(impression);
            groupingImpressions.put(impression.feature, featureImpressions);
        }

        List<TestImpressions> groupedImpressions = new ArrayList<>();
        for(Map.Entry<String, List<KeyImpression>> entry : groupingImpressions.entrySet()) {
            TestImpressions testImpressions = new TestImpressions();
            testImpressions.testName = entry.getKey();
            testImpressions.keyImpressions = entry.getValue();
            groupedImpressions.add(testImpressions);
        }

        return groupedImpressions;

    }
}
