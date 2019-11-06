package io.split.android.client;

import android.content.Context;
import androidx.annotation.VisibleForTesting;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import io.split.android.client.localhost.LocalhostFileParser;
import io.split.android.client.localhost.LocalhostPropertiesFileParser;
import io.split.android.client.localhost.LocalhostYamlFileParser;
import io.split.android.client.dtos.Split;
import io.split.android.client.storage.legacy.FileStorage;
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

    static final String DEFAULT_SPLITS_FILENAME = "splits";
    static final String LOCALHOST = "localhost";
    static final String LOCALHOST_FOLDER = "localhost";
    static final String PROPERTIES_EXTENSION = "properties";
    static final String YML_EXTENSION = "yml";
    static final String YAML_EXTENSION = "yaml";

    private final LocalhostSplitClient mClient;
    private final LocalhostSplitManager mManager;
    private boolean mIsSdkReady;

    private String mLocalhostFileName = DEFAULT_SPLITS_FILENAME;

    public static LocalhostSplitFactory createLocalhostSplitFactory(String key, Context context) throws IOException {
        return new LocalhostSplitFactory(key, context);
    }

    public LocalhostSplitFactory(String key, Context context) throws IOException {
        this(key, context, null);
    }

    @VisibleForTesting
    public LocalhostSplitFactory(String key, Context context, String localhostFileName) throws IOException {

        if(localhostFileName != null) {
            mLocalhostFileName = localhostFileName;
        }

        Map<String, Split> featureToTreatmentMap;
        LocalhostFileParser parser;
        String yamlName = getYamlFileName(context);
        if(yamlName != null) {
            FileStorage fileStorage = new FileStorage(context.getCacheDir(), LOCALHOST_FOLDER);
            copyYamlFileResourceToDataFolder(yamlName, fileStorage, context);
            parser = new LocalhostYamlFileParser(fileStorage);
            featureToTreatmentMap = parser.parse(yamlName);
        } else {
            parser = new LocalhostPropertiesFileParser(context);
            featureToTreatmentMap = parser.parse(mLocalhostFileName + "." + PROPERTIES_EXTENSION);
            Logger.w("Localhost mode: .split mocks will be deprecated soon in favor of YAML files, which provide more targeting power. Take a look in our documentation.");
        }
        ImmutableMap<String, Split> splits;
        if (featureToTreatmentMap != null) {
            mIsSdkReady = true;
            splits = ImmutableMap.copyOf(featureToTreatmentMap);
        } else {
            mIsSdkReady = false;
            splits = ImmutableMap.of();
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
        mClient.updateSplitsMap(ImmutableMap.of());
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

    private void copyYamlFileResourceToDataFolder(String fileName, FileStorage fileStorage, Context context) {
        FileUtils fileUtils = new FileUtils();
        String yamlContent;
        try {
            yamlContent = fileUtils.loadFileContent(fileName, context);
            if(yamlContent != null) {
                fileStorage.write(fileName, yamlContent);
            }
        } catch (IOException e) {
            Logger.e(e.getLocalizedMessage());
        }
    }

    private String getYamlFileName(Context context) {
        String fileName = mLocalhostFileName + "." + YAML_EXTENSION;
        FileUtils fileUtils = new FileUtils();
        if(fileUtils.fileExists(fileName, context)) {
            return  fileName;
        }

        fileName = mLocalhostFileName + "." + YML_EXTENSION;
        if(fileUtils.fileExists(mLocalhostFileName + "." + YML_EXTENSION, context)) {
            return fileName;
        }
        return null;
    }

}