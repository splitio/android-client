package io.split.android.client.validators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.SplitResult;
import io.split.android.client.utils.Logger;
import io.split.android.engine.metrics.Metrics;
import io.split.android.grammar.Treatments;

public interface TreatmentManager {

    String getTreatment(String split, Map<String, Object> attributes, boolean isClientDestroyed, boolean isSdkReadyFired);

    SplitResult getTreatmentWithConfig(String split, Map<String, Object> attributes, boolean isClientDestroyed, boolean isSdkReadyFired);

    Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes, boolean isClientDestroyed, boolean isSdkReadyFired);

    Map<String, SplitResult> getTreatmentsWithConfig(List<String> splits, Map<String, Object> attributes, boolean isClientDestroyed, boolean isSdkReadyFired);
}
