package io.split.android.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.grammar.Treatments;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A SplitClient that ensures that all features are turned off for all users.
 * Useful for testing
 *
 */
public class AlwaysReturnControlSplitClient implements io.split.android.client.SplitClient {

    @Override
    public String getTreatment(String featureFlagName) {
        return Treatments.CONTROL;
    }

    @Override
    public Map<String, String> getTreatments(List<String> featureFlagNames, Map<String, Object> attributes) {
        Map<String, String> results = new HashMap<>();
        if(featureFlagNames == null) {
            return results;
        }

        for(String featureFlagName : featureFlagNames) {
            results.put(featureFlagName, Treatments.CONTROL);
        }
        return results;
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> featureFlagNames, Map<String, Object> attributes) {
        Map<String, SplitResult> results = new HashMap<>();
        if(featureFlagNames == null) {
            return results;
        }

        for(String featureFlagName : featureFlagNames) {
            results.put(featureFlagName, new SplitResult(Treatments.CONTROL));
        }
        return results;
    }

    @Override
    public String getTreatment(String featureFlagName, Map<String, Object> attributes) {
        return Treatments.CONTROL;
    }

    @Override
    public SplitResult getTreatmentWithConfig(String featureFlagName, Map<String, Object> attributes) {
        return new SplitResult(Treatments.CONTROL);
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes) {
        return Collections.singletonMap(flagSet, Treatments.CONTROL); //TODO
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes) {
        return Collections.singletonMap(flagSets.get(0), Treatments.CONTROL); //TODO
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes) {
        return Collections.singletonMap(flagSet, new SplitResult(Treatments.CONTROL)); //TODO
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes) {
        return Collections.singletonMap(flagSets.get(0), new SplitResult(Treatments.CONTROL)); //TODO
    }

    @Override
    public boolean setAttribute(String attributeName, Object value) {
        return true;
    }

    @Nullable
    @Override
    public Object getAttribute(String attributeName) {
        return null;
    }

    @Override
    public boolean setAttributes(Map<String, Object> attributes) {
        return true;
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

    @Override
    public void destroy() {

    }

    @Override
    public void flush() {

    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void on(SplitEvent event, SplitEventTask task) {
    }

    @Override
    public boolean track(String trafficType, String eventType) {
        return false;
    }

    @Override
    public boolean track(String trafficType, String eventType, double value) {
        return false;
    }

    @Override
    public boolean track(String eventType) {
        return false;
    }

    @Override
    public boolean track(String eventType, double value) {
        return false;
    }


    @Override
    public boolean track(String trafficType, String eventType, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean track(String trafficType, String eventType, double value, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean track(String eventType, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean track(String eventType, double value, Map<String, Object> properties) {
        return false;
    }
}
