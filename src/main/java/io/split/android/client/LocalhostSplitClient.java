package io.split.android.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import io.split.android.client.Localhost.LocalhostGrammar;
import io.split.android.client.dtos.Split;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.grammar.Treatments;

import java.util.HashMap;
import java.util.List;
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
    private LocalhostSplitFactory mfactory;
    private ImmutableMap<String, Split> mFeatureToTreatmentMap;
    private String mKey;
    private LocalhostGrammar mLocalhostGrammar;

    public LocalhostSplitClient(LocalhostSplitFactory container, String key, ImmutableMap<String, Split> featureToTreatmentMap) {
        mFeatureToTreatmentMap = featureToTreatmentMap;
        mfactory = container;
        mKey = key;
        mLocalhostGrammar = new LocalhostGrammar();
    }

    @Override
    public String getTreatment(String split) {
        if (mKey == null || split == null) {
            return Treatments.CONTROL;
        }

        Split splitDefinition = mFeatureToTreatmentMap.get(mLocalhostGrammar.buildSplitKeyName(split, mKey));
        if(splitDefinition == null) {
            splitDefinition = mFeatureToTreatmentMap.get(split);
        }

        if (splitDefinition == null || splitDefinition.defaultTreatment == null) {
            return Treatments.CONTROL;
        }

        return splitDefinition.defaultTreatment;
    }

    @Override
    public String getTreatment(String split, Map<String, Object> attributes) {
        return getTreatment(split);
    }

    @Override
    public SplitResult getTreatmentWithConfig(String split, Map<String, Object> attributes) {
        if (mKey == null || split == null) {
            return new SplitResult(Treatments.CONTROL);
        }

        Split splitDefinition = mFeatureToTreatmentMap.get(mLocalhostGrammar.buildSplitKeyName(split, mKey));
        if(splitDefinition == null) {
            splitDefinition = mFeatureToTreatmentMap.get(split);
        }

        if (splitDefinition == null || splitDefinition.defaultTreatment == null) {
            return new SplitResult(Treatments.CONTROL);
        }

        String config = null;
        if (splitDefinition.configurations != null) {
            config = splitDefinition.configurations.get(splitDefinition.defaultTreatment);
        }

        return new SplitResult(splitDefinition.defaultTreatment, config);
    }

    @Override
    public Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes) {
        return getTreatments(splits, attributes);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> splits, Map<String, Object> attributes) {

        Map<String, SplitResult> result = new HashMap<String, SplitResult>();
        for(String split : splits) {
            result.put(split, new SplitResult(getTreatment(split)));
        }

        return result;
    }


    @Override
    public void destroy() {
        mfactory.destroy();
    }

    @Override
    public void flush() {
        return;
    }

    @Override
    public boolean isReady() {
        return mfactory.isReady();
    }

    public void on(SplitEvent event, SplitEventTask task) {
        return;
    }

    void updateSplitsMap(ImmutableMap<String, Split> splits) {
        mFeatureToTreatmentMap = splits;
    }

    @VisibleForTesting
    ImmutableMap<String, Split> featureToTreatmentMap() {
        return mFeatureToTreatmentMap;
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
