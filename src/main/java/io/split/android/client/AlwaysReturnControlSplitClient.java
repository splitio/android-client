package io.split.android.client;

import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.grammar.Treatments;
import java.util.HashMap;
import java.util.Map;

/**
 * A SplitClient that ensures that all features are turned off for all users.
 * Useful for testing
 *
 */
public class AlwaysReturnControlSplitClient implements io.split.android.client.SplitClient {

    @Override
    public String getTreatment(String split) {
        return Treatments.CONTROL;
    }

    @Override
    public String getTreatment(String split, Map<String, Object> attributes) {
        return Treatments.CONTROL;
    }

    @Override
    public Map<String, String> getTreatments(String[] splits, Map<String, Object> attributes) {
        Map<String, String> results = new HashMap<>();
        if(splits == null) {
            return results;
        }

        for(String split : splits) {
            results.put(split, Treatments.CONTROL);
        }
        return results;
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
        return;
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


}
