package io.split.android.client;

import android.content.Context;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import java.io.FileNotFoundException;
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

    private boolean _ready;

    public static LocalhostSplitFactory createLocalhostSplitFactory(String key, Context context) throws IOException {
        return new LocalhostSplitFactory(key, context);
    }

    public LocalhostSplitFactory(String key, Context context) throws IOException {

        Map<String, String> _featureToTreatmentMap = new HashMap<>();

        try {
            _ready = true;
            Properties _properties = new Properties();
            _properties.load(context.getAssets().open(FILENAME));
            for (Object k: _properties.keySet()) {
                _featureToTreatmentMap.put((String) k,_properties.getProperty((String) k));
            }
        } catch (FileNotFoundException e) {
            _ready = false;
            Timber.e("File not found. Add split.properties in your application assets");
        } catch (Exception e){
            _ready = false;
            Timber.e(e.getMessage());
        }

        _client = new LocalhostSplitClient(this, key, _featureToTreatmentMap);
        _manager = new LocalhostSplitManager(_featureToTreatmentMap);

    }

    @Override
    public SplitClient client() {
        return _client;
    }


    private SplitManager manager() {
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

    @Override
    public boolean isReady() {
        return _ready;
    }

    public void updateFeatureToTreatmentMap(Map<String, String> featureToTreatmentMap) {
        _client.updateFeatureToTreatmentMap(featureToTreatmentMap);
        _manager.updateFeatureToTreatmentMap(featureToTreatmentMap);
    }
}
