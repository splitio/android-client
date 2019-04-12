package io.split.android.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.split.android.client.api.SplitView;
import io.split.android.client.dtos.Split;

import java.util.ArrayList;
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
public final class LocalhostSplitManager implements SplitManager {

    private ImmutableMap<String, Split> mFeatureToTreatmentMap;

    public LocalhostSplitManager(ImmutableMap<String, Split> featureToTreatmentMap) {
        mFeatureToTreatmentMap = featureToTreatmentMap;
    }

    @Override
    public List<SplitView> splits() {
        List<SplitView> result = new ArrayList<SplitView>();

        for (Map.Entry<String, Split> entry : mFeatureToTreatmentMap.entrySet()) {
            result.add(toSplitView(entry.getKey(), entry.getValue()));
        }

        return result;
    }

    @Override
    public List<String> splitNames() {
        return Lists.newArrayList(mFeatureToTreatmentMap.keySet());
    }

    @Override
    public void destroy() {
    }

    @Override
    public SplitView split(String featureName) {
        if (!mFeatureToTreatmentMap.containsKey(featureName)) {
            return null;
        }

        return toSplitView(featureName, mFeatureToTreatmentMap.get(featureName));
    }

    void updateSplitsMap(ImmutableMap<String, Split> featureToTreatmentMap) {
        mFeatureToTreatmentMap = featureToTreatmentMap;
    }

    @VisibleForTesting
    ImmutableMap<String, Split> featureToTreatmentMap() {
        return mFeatureToTreatmentMap;
    }

    private SplitView toSplitView(String featureName, Split split) {
        SplitView view = new SplitView();
        view.name = featureName;
        view.killed = false;
        view.trafficType = null;
        view.changeNumber = 0;
        view.treatments = new ArrayList<String>();
        if (split != null && split.defaultTreatment != null) {
            view.treatments.add(split.defaultTreatment);
        }
        view.configs = split.configurations;

        return view;
    }


}
