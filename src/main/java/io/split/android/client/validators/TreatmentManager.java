package io.split.android.client.validators;

import java.util.List;
import java.util.Map;
import io.split.android.client.SplitResult;

public interface TreatmentManager {

    String getTreatment(String split, Map<String, Object> attributes, boolean isClientDestroyed);

    SplitResult getTreatmentWithConfig(String split, Map<String, Object> attributes, boolean isClientDestroyed);

    Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes, boolean isClientDestroyed);

    Map<String, SplitResult> getTreatmentsWithConfig(List<String> splits, Map<String, Object> attributes, boolean isClientDestroyed);
}
