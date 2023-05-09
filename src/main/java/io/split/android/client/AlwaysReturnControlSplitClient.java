package io.split.android.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.grammar.Treatments;
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
    public String getTreatment(String featureFlag) {
        return Treatments.CONTROL;
    }

    @Override
    public Map<String, String> getTreatments(List<String> featureFlags, Map<String, Object> attributes) {
        Map<String, String> results = new HashMap<>();
        if(featureFlags == null) {
            return results;
        }

        for(String featureFlag : featureFlags) {
            results.put(featureFlag, Treatments.CONTROL);
        }
        return results;
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> featureFlags, Map<String, Object> attributes) {
        Map<String, SplitResult> results = new HashMap<>();
        if(featureFlags == null) {
            return results;
        }

        for(String featureFlag : featureFlags) {
            results.put(featureFlag, new SplitResult(Treatments.CONTROL));
        }
        return results;
    }

    @Override
    public String getTreatment(String featureFlag, Map<String, Object> attributes) {
        return Treatments.CONTROL;
    }

    @Override
    public SplitResult getTreatmentWithConfig(String featureFlag, Map<String, Object> attributes) {
        return new SplitResult(Treatments.CONTROL);
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
