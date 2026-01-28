package io.split.android.client.service.impressions.unique;

import java.util.Map;
import java.util.Set;

public interface UniqueKeysTracker {

    boolean track(String key, String featureName);

    Map<String, Set<String>> popAll();

    boolean isFull();
}
