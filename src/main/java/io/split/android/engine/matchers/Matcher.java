package io.split.android.engine.matchers;

import io.split.android.client.SplitClientImpl;

import java.util.Map;

public interface Matcher {
    boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, SplitClientImpl splitClient);
}
