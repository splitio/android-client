package io.split.android.client;

import android.content.Context;

import com.google.common.collect.ImmutableMap;

import java.io.IOException;
import java.util.Map;

import io.split.android.client.Localhost.LocalhostFileParser;
import io.split.android.client.Localhost.LocalhostPropertiesFileParser;
import io.split.android.client.Localhost.LocalhostYamlFileParser;
import io.split.android.client.dtos.Split;
import io.split.android.client.storage.ResourcesFileStorage;
import io.split.android.client.utils.Logger;

/**
 * An implementation of SplitClient that considers all partitions
 * passed in the constructor to be 100% on for all users, and
 * any other split to be 100% off for all users. This implementation
 * is useful for using Codigo in localhost environment.
 *
 */
public final class LocalhostSplitFactory implements SplitFactory {

    static final String SPLITS_YAML_FILENAME = "split.yaml";
    static final String SPLITS_PROPERTIES_FILENAME = "split.properties";
    static final String LOCALHOST = "localhost";

    private final LocalhostSplitClient mClient;
    private final LocalhostSplitManager mManager;
    private boolean mIsSdkReady;

    public static LocalhostSplitFactory createLocalhostSplitFactory(String key, Context context) throws IOException {
        return new LocalhostSplitFactory(key, context);
    }

    public LocalhostSplitFactory(String key, Context context) throws IOException {

        LocalhostFileParser parser = new LocalhostYamlFileParser(new ResourcesFileStorage());
        Map<String, Split> featureToTreatmentMap = parser.parse(SPLITS_YAML_FILENAME);
        if (featureToTreatmentMap == null) {
            parser = new LocalhostPropertiesFileParser();
            featureToTreatmentMap = parser.parse(SPLITS_PROPERTIES_FILENAME);
        }

        mIsSdkReady = (featureToTreatmentMap != null);

        mClient = new LocalhostSplitClient(this, key, featureToTreatmentMap);
        mManager = new LocalhostSplitManager(featureToTreatmentMap);

        Logger.i("Android SDK initialized!");
    }

    @Override
    public SplitClient client() {
        return mClient;
    }

    @Override
    public SplitManager manager() {
        return mManager;
    }

    @Override
    public void destroy() {
        mClient.updateFeatureToTreatmentMap(ImmutableMap.<String, String>of());
    }

    @Override
    public void flush() {
        mClient.flush();
    }

    @Override
    public boolean isReady() {
        return mIsSdkReady;
    }

    public void updateFeatureToTreatmentMap(Map<String, String> featureToTreatmentMap) {
        mClient.updateFeatureToTreatmentMap(featureToTreatmentMap);
        mManager.updateFeatureToTreatmentMap(featureToTreatmentMap);
    }

}
