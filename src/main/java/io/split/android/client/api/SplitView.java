package io.split.android.client.api;

import java.util.List;
import java.util.Map;

import io.split.android.client.SplitManager;

/**
 * A view of a feature flag, meant for consumption through {@link SplitManager} interface.
 *
 */
public class SplitView {
    public String name;
    public String trafficType;
    public boolean killed;
    public List<String> treatments;
    public long changeNumber;
    public Map<String, String> configs;
    public List<String> sets;
}
