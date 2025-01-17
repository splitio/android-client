package io.split.android.client.api;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.split.android.client.SplitManager;

/**
 * A view of a feature flag, meant for consumption through {@link SplitManager} interface.
 */
public class SplitView {
    public String name;
    public String trafficType;
    public boolean killed;
    public List<String> treatments;
    public long changeNumber;
    public Map<String, String> configs;
    @NonNull
    public List<String> sets = new ArrayList<>();
    public String defaultTreatment;
    public boolean impressionsDisabled;
}
