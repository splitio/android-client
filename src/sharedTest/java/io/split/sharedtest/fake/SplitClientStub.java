package io.split.sharedtest.fake;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.EvaluationOptions;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitResult;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;

public class SplitClientStub implements SplitClient {
    @Override
    public String getTreatment(String featureFlagName) {
        return getTreatment(featureFlagName);
    }

    @Override
    public String getTreatment(String featureFlagName, Map<String, Object> attributes) {
        return "control";
    }

    @Override
    public String getTreatment(String featureFlagName, Map<String, Object> attributes, EvaluationOptions evaluationOptions) {
        return getTreatment(featureFlagName, attributes);
    }

    @Override
    public SplitResult getTreatmentWithConfig(String featureFlagName, Map<String, Object> attributes) {
        return getTreatmentWithConfig(featureFlagName, attributes, null);
    }

    @Override
    public SplitResult getTreatmentWithConfig(String featureFlagName, Map<String, Object> attributes, EvaluationOptions evaluationOptions) {
        return null;
    }

    @Override
    public Map<String, String> getTreatments(List<String> featureFlagNames, Map<String, Object> attributes) {
        return getTreatments(featureFlagNames, attributes, null);
    }

    @Override
    public Map<String, String> getTreatments(List<String> featureFlagNames, Map<String, Object> attributes, EvaluationOptions evaluationOptions) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> featureFlagNames, Map<String, Object> attributes) {
        return getTreatmentsWithConfig(featureFlagNames, attributes, null);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> featureFlagNames, Map<String, Object> attributes, EvaluationOptions evaluationOptions) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes) {
        return getTreatmentsByFlagSet(flagSet, attributes, null);
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes, EvaluationOptions evaluationOptions) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes) {
        return getTreatmentsByFlagSets(flagSets, attributes, null);
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes, EvaluationOptions evaluationOptions) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes) {
        return getTreatmentsWithConfigByFlagSet(flagSet, attributes, null);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes, EvaluationOptions evaluationOptions) {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes) {
        return getTreatmentsWithConfigByFlagSets(flagSets, attributes, null);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes, EvaluationOptions evaluationOptions) {
        return Collections.emptyMap();
    }

    @Override
    public void destroy() {

    }

    @Override
    public void flush() {

    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void on(SplitEvent event, SplitEventTask task) {

    }

    @Override
    public boolean track(String eventType) {
        return false;
    }

    @Override
    public boolean track(String trafficType, String eventType, double value) {
        return false;
    }

    @Override
    public boolean track(String trafficType, String eventType) {
        return false;
    }

    @Override
    public boolean track(String eventType, double value) {
        return false;
    }

    @Override
    public boolean track(String eventType, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean track(String trafficType, String eventType, double value, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean track(String trafficType, String eventType, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean track(String eventType, double value, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean setAttribute(String attributeName, Object value) {
        return false;
    }

    @Nullable
    @Override
    public Object getAttribute(String attributeName) {
        return null;
    }

    @Override
    public boolean setAttributes(Map<String, Object> attributes) {
        return false;
    }

    @NonNull
    @Override
    public Map<String, Object> getAllAttributes() {
        return new HashMap<>();
    }

    @Override
    public boolean removeAttribute(String attributeName) {
        return true;
    }

    @Override
    public boolean clearAttributes() {
        return true;
    }
}
