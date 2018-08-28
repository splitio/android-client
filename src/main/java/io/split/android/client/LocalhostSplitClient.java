package io.split.android.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.grammar.Treatments;

import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An implementation of SplitClient that considers all partitions
 * passed in the constructor to be 100% on for all users, and
 * any other split to be 100% off for all users. This implementation
 * is useful for using Codigo in localhost environment.
 *
 */
public final class LocalhostSplitClient implements SplitClient {
    private LocalhostSplitFactory _container;
    private ImmutableMap<String, String> _featureToTreatmentMap;
    private String key;

    public LocalhostSplitClient(LocalhostSplitFactory container, String key, Map<String, String> featureToTreatmentMap) {
        checkNotNull(featureToTreatmentMap, "featureToTreatmentMap must not be null");
        _featureToTreatmentMap = ImmutableMap.copyOf(featureToTreatmentMap);
        _container = container;
        this.key = key;
    }

    @Override
    public String getTreatment(String split) {
        if (key == null || split == null) {
            return Treatments.CONTROL;
        }

        String treatment = _featureToTreatmentMap.get(split);

        if (treatment == null) {
            return Treatments.CONTROL;
        }

        return treatment;
    }

    @Override
    public String getTreatment(String split, Map<String, Object> attributes) {
        return getTreatment(split);
    }

    @Override
    public Map<String, String> getTreatments(String[] splits, Map<String, Object> attributes) {
        return getTreatments(splits, attributes);
    }


    @Override
    public void destroy() {
        _container.destroy();
    }

    @Override
    public void flush() {
        return;
    }

    @Override
    public boolean isReady() {
        return _container.isReady();
    }

    public void on(SplitEvent event, SplitEventTask task) {
        return;
    }

    void updateFeatureToTreatmentMap(Map<String, String> featureToTreatmentMap) {
        checkNotNull(featureToTreatmentMap, "featureToTreatmentMap must not be null");
        _featureToTreatmentMap = ImmutableMap.copyOf(featureToTreatmentMap);
    }

    @VisibleForTesting
    ImmutableMap<String, String> featureToTreatmentMap() {
        return _featureToTreatmentMap;
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
