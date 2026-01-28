package helper;

import java.util.HashMap;
import java.util.Map;

import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;

public class ImpressionListenerHelper implements ImpressionListener {

    Map<String, Impression> impressions;

    public ImpressionListenerHelper() {
        impressions = new HashMap<>();
    }

    @Override
    public void log(Impression impression) {
        String mapKey = buildKey(impression.key(), impression.split(),impression.treatment());
        impressions.put(mapKey, impression);
    }

    @Override
    public void close() {
    }

    public int impressionCount() {
        return impressions.size();
    }

    public Impression getImpression(String key) {
        return impressions.get(key);
    }

    public String buildKey(String key, String splitName, String treatment) {
        return key + "_" + splitName + "_" + treatment;
    }
}
