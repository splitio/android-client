package io.split.android.client;

import android.content.Context;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import timber.log.Timber;

/**
 * An implementation of SplitClient that considers all partitions
 * passed in the constructor to be 100% on for all users, and
 * any other split to be 100% off for all users. This implementation
 * is useful for using Codigo in localhost environment.
 *
 */
public final class LocalhostSplitFactory implements SplitFactory {

    static final String FILENAME = "split.properties";
    static final String LOCALHOST = "localhost";

    private final LocalhostSplitClient _client;
    private final LocalhostSplitManager _manager;

    public static LocalhostSplitFactory createLocalhostSplitFactory(String key, Context context) throws IOException {
        String directory = System.getProperty("user.home");
        Preconditions.checkNotNull(directory, "Property user.home should be set when using environment: " + LOCALHOST);
        return new LocalhostSplitFactory(directory, key, context);
    }

    public LocalhostSplitFactory(String directory, String key, Context context) throws IOException {
        Preconditions.checkNotNull(directory, "directory must not be null");

        Timber.i("home = %s",directory);

        Map<String, String> _featureToTreatmentMap = new HashMap<>();

        Properties _properties = new Properties();
        _properties.load(context.getAssets().open(FILENAME));
        for (Object k: _properties.keySet()) {
            _featureToTreatmentMap.put((String) k,_properties.getProperty((String) k));
        }

        _client = new LocalhostSplitClient(this, key, _featureToTreatmentMap);
        _manager = new LocalhostSplitManager(_featureToTreatmentMap);

    }

    @Override
    public SplitClient client() {
        return _client;
    }

    @Override
    public SplitManager manager() {
        return _manager;
    }

    @Override
    public void destroy() {
        _client.updateFeatureToTreatmentMap(ImmutableMap.<String, String>of());
    }

    @Override
    public void flush() {
        _client.flush();
    }

    public void updateFeatureToTreatmentMap(Map<String, String> featureToTreatmentMap) {
        _client.updateFeatureToTreatmentMap(featureToTreatmentMap);
        _manager.updateFeatureToTreatmentMap(featureToTreatmentMap);
    }
}
