package io.split.android.client.validators;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

import io.split.android.client.EvaluationOptions;
import io.split.android.client.SplitResult;

public interface TreatmentManager {

    String getTreatment(String split, Map<String, Object> attributes, boolean isClientDestroyed);

    SplitResult getTreatmentWithConfig(String split, Map<String, Object> attributes, boolean isClientDestroyed);

    Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes, boolean isClientDestroyed);

    Map<String, SplitResult> getTreatmentsWithConfig(List<String> splits, Map<String, Object> attributes, boolean isClientDestroyed);

    Map<String, String> getTreatmentsByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes, boolean isClientDestroyed);

    Map<String, String> getTreatmentsByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes, boolean isClientDestroyed);

    Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes, boolean isClientDestroyed);

    Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes, boolean isClientDestroyed);


    // temporary methods to reduce changes in this iteration
    String getTreatment(String split, Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed);

    SplitResult getTreatmentWithConfig(String split, Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed);

    Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed);

    Map<String, SplitResult> getTreatmentsWithConfig(List<String> splits, Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed);

    Map<String, String> getTreatmentsByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed);

    Map<String, String> getTreatmentsByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed);

    Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed);

    Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes, EvaluationOptions evaluationOptions, boolean isClientDestroyed);
}
