package io.split.android.client;

import android.content.Context;
import android.support.annotation.VisibleForTesting;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.split.android.client.Localhost.LocalhostFileParser;
import io.split.android.client.Localhost.LocalhostPropertiesFileParser;
import io.split.android.client.Localhost.LocalhostYamlFileParser;
import io.split.android.client.dtos.Split;
import io.split.android.client.storage.FileStorage;
import io.split.android.client.utils.FileUtils;
import io.split.android.client.utils.Logger;

/**
 * An implementation of SplitClient that considers all partitions
 * passed in the constructor to be 100% on for all users, and
 * any other split to be 100% off for all users. This implementation
 * is useful for using Codigo in localhost environment.
 *
 */
public final class LocalhostSplitFactory implements SplitFactory {

    static final String DEFAULT_SPLITS_YAML_FILENAME = "splits.yaml";
    static final String DEFAULT_SPLITS_PROPERTIES_FILENAME = "splits.properties";
    static final String LOCALHOST = "localhost";
    static final String LOCALHOST_FOLDER = "localhost";

    private final LocalhostSplitClient mClient;
    private final LocalhostSplitManager mManager;
    private boolean mIsSdkReady;

    private String mLocalhostYamlFileName = DEFAULT_SPLITS_YAML_FILENAME;
    private String mLocalhostPropertiesFileName = DEFAULT_SPLITS_PROPERTIES_FILENAME;

    public static LocalhostSplitFactory createLocalhostSplitFactory(String key, Context context) throws IOException {
        return new LocalhostSplitFactory(key, context);
    }

    public LocalhostSplitFactory(String key, Context context) throws IOException {
        this(key, context, null);
    }

    @VisibleForTesting
    public LocalhostSplitFactory(String key, Context context, String localhostFileName) throws IOException {

        if(localhostFileName != null) {
            mLocalhostYamlFileName = localhostFileName + ".yaml";
            mLocalhostPropertiesFileName = localhostFileName + ".properties";
        }

        FileStorage fileStorage = new FileStorage(context, LOCALHOST_FOLDER);
        copyYamlFileResourceToDataFolder(fileStorage, context);
        LocalhostFileParser parser = new LocalhostYamlFileParser(fileStorage);
        Map<String, Split> featureToTreatmentMap = parser.parse(mLocalhostYamlFileName);
        if (featureToTreatmentMap == null) {
            parser = new LocalhostPropertiesFileParser(context);
            featureToTreatmentMap = parser.parse(mLocalhostPropertiesFileName);
        }
        ImmutableMap<String, Split> splits;
        if (featureToTreatmentMap != null) {
            mIsSdkReady = true;
            splits = ImmutableMap.copyOf(featureToTreatmentMap);
        } else {
            mIsSdkReady = false;
            splits = ImmutableMap.<String, Split>of();
            Logger.w("Neither yaml file nor properties were found. Localhost feature map is empty.");
        }

        mClient = new LocalhostSplitClient(this, key, splits);
        mManager = new LocalhostSplitManager(splits);

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
        mClient.updateSplitsMap(ImmutableMap.<String, Split>of());
    }

    @Override
    public void flush() {
        mClient.flush();
    }

    @Override
    public boolean isReady() {
        return mIsSdkReady;
    }

    @Deprecated
    public void updateFeatureToTreatmentMap(Map<String, String> featureToTreatmentMap) {
        ImmutableMap<String, Split> splits = convertFeatureNamesMapToSplits(featureToTreatmentMap);
        mClient.updateSplitsMap(splits);
        mManager.updateSplitsMap(splits);
    }

    public void updateSplitsMap(Map<String, Split> splits) {
        ImmutableMap<String, Split> immutableSplits = ImmutableMap.copyOf(splits);
        mClient.updateSplitsMap(immutableSplits);
        mManager.updateSplitsMap(immutableSplits);
    }

    private ImmutableMap<String, Split> convertFeatureNamesMapToSplits(Map<String, String> features) {
        Map<String, Split> splits = new HashMap<>();
        if(features != null) {
            for (Map.Entry<String, String> entry : features.entrySet()) {
                System.out.println(entry.getKey() + "/" + entry.getValue());
                if (entry.getKey() != null && entry.getValue() != null) {
                    Split split = new Split();
                    split.name = entry.getKey();
                    split.defaultTreatment = entry.getValue();
                    splits.put(entry.getKey(), split);
                }
            }
        }
        return ImmutableMap.copyOf(splits);
    }

    private void copyYamlFileResourceToDataFolder(FileStorage fileStorage, Context context) {

        FileUtils fileUtils = new FileUtils();
        String yamlContent = null;
        try {
            yamlContent = fileUtils.loadFileContent(mLocalhostYamlFileName, context);
            if(yamlContent != null) {
                fileStorage.write(mLocalhostYamlFileName, yamlContent);
            }
        } catch (IOException e) {
            Logger.e(e.getLocalizedMessage());
        }
    }

}