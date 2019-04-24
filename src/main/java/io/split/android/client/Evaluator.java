package io.split.android.client;

import java.util.Map;

public interface Evaluator {
    public EvaluationResult getTreatment(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes);
}
