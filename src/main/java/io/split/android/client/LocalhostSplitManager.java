package io.split.android.client;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import io.split.android.client.Localhost.LocalhostGrammar;
import io.split.android.client.api.SplitView;
import io.split.android.client.dtos.Split;
import io.split.android.grammar.Treatments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private LocalhostGrammar mLocalhostGrammar;

    public LocalhostSplitManager(ImmutableMap<String, Split> featureToTreatmentMap) {
        mFeatureToTreatmentMap = featureToTreatmentMap;
        mLocalhostGrammar = new LocalhostGrammar();
    }

    @Override
    public List<SplitView> splits() {
        return getSplitViews(null);
    }

    @Override
    public List<String> splitNames() {
        Set<String> names = new HashSet<>();
        for (String key : mFeatureToTreatmentMap.keySet()) {
            names.add(mLocalhostGrammar.getSplitName(key));
        }
        return new ArrayList<>(names);
    }

    @Override
    public void destroy() {
    }

    @Override
    public SplitView split(String featureName) {
        List<SplitView> splitViews = getSplitViews(featureName);
        if(splitViews.size() > 0 ) {
            return  splitViews.get(0);
        }
        return null;
    }

    void updateSplitsMap(ImmutableMap<String, Split> featureToTreatmentMap) {
        mFeatureToTreatmentMap = featureToTreatmentMap;
    }

    @VisibleForTesting
    ImmutableMap<String, Split> featureToTreatmentMap() {
        return mFeatureToTreatmentMap;
    }

    private SplitView toSplitView(Split split, List<String> treatments) {
        SplitView view = new SplitView();
        view.name = split.name;
        view.killed = false;
        view.trafficType = null;
        view.changeNumber = 0;
        view.treatments = new ArrayList<String>();
        if (split != null && treatments != null) {
            view.treatments.addAll(treatments);
        }
        view.configs = split.configurations;

        return view;
    }

    private List<SplitView> getSplitViews(String splitName) {
        Map<String, List<String>> treatmentsForSplit = new HashMap<>();
        Map<String, Split> splits = new HashMap<>();
        for (Map.Entry<String, Split> entry : mFeatureToTreatmentMap.entrySet()) {
            String splitNameOnly = mLocalhostGrammar.getSplitName(entry.getKey());
            if(splitNameOnly != null && (splitName == null || (splitName != null && splitName.equals(splitNameOnly)))) {
                List<String> treatments = treatmentsForSplit.get(splitNameOnly);
                if(treatments == null) {
                    Split split = new Split();
                    split.name = splitNameOnly;
                    split.defaultTreatment = Treatments.CONTROL;
                    split.configurations = entry.getValue().configurations;
                    treatments = new ArrayList<>();
                    treatments.add(entry.getValue().defaultTreatment);
                    treatmentsForSplit.put(splitNameOnly, treatments);
                    splits.put(splitNameOnly, split);
                } else {
                    Split split = splits.get(splitNameOnly);
                    Split entrySplit = entry.getValue();
                    if(entrySplit.configurations != null) {
                        if(split.configurations == null) {
                            split.configurations = new HashMap<>();
                        }
                        split.configurations.putAll(entrySplit.configurations);
                    }
                    treatments.add(entry.getValue().defaultTreatment);
                }
            }
        }

        List<SplitView> splitViews = new ArrayList<>();
        for (Split split : splits.values()) {
            splitViews.add(toSplitView(split, treatmentsForSplit.get(split.name)));
        }

        return splitViews;
    }

}
