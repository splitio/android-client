package io.split.android.client;

import androidx.annotation.Nullable;

import java.util.Map;

import io.split.android.client.telemetry.model.Method;

public interface Evaluator {
    EvaluationResult getTreatment(String matchingKey, String bucketingKey, String split, Map<String, Object> attributes, @Nullable Method callingMethod);
}
