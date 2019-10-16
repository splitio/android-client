package io.split.android.engine.matchers;

import io.split.android.client.Evaluator;

import java.util.Map;

public interface Matcher {
    boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, Evaluator splitClient);
}
