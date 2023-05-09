package io.split.android.client;

import io.split.android.client.api.SplitView;

import java.util.List;

/**
 * An interface to manage an instance of Split SDK.
 */
public interface SplitManager {

    /**
     * Retrieves the feature flags that are currently registered with the
     * SDK.
     *
     * @return a List of SplitView or empty
     */
    List<SplitView> splits();

    /**
     * Returns the feature flag registered with the SDK of this name.
     *
     * @return SplitView or null
     */
    SplitView split(String featureFlagName);

    /**
     * Returns the names of feature flags registered with the SDK.
     *
     * @return a List of String (Split Feature Names) or empty
     */
    List<String> splitNames();

    /**
     * Makes functions return empty or null values
     */
    void destroy();

}
