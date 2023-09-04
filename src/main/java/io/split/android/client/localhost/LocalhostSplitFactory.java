package io.split.android.client.localhost;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.split.android.client.FilterBuilder;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFilter;
import io.split.android.client.SplitManager;
import io.split.android.client.SplitManagerImpl;
import io.split.android.client.SyncConfig;
import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManagerFactory;
import io.split.android.client.attributes.AttributesManagerFactoryImpl;
import io.split.android.client.attributes.AttributesMergerImpl;
import io.split.android.client.events.EventsManagerCoordinator;
import io.split.android.client.localhost.shared.LocalhostSplitClientContainerImpl;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutorImpl;
import io.split.android.client.shared.SplitClientContainer;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.storage.legacy.FileStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.NoOpTelemetryStorage;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.validators.AttributesValidatorImpl;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.client.validators.ValidationMessageLoggerImpl;
import io.split.android.engine.experiments.SplitParser;

/**
 * An implementation of SplitClient that considers all partitions
 * passed in the constructor to be 100% on for all users, and
 * any other split to be 100% off for all users. This implementation
 * is useful for using Split in localhost environment.
 */
public class LocalhostSplitFactory implements SplitFactory {

    private final SplitManager mManager;
    private final LocalhostSynchronizer mSynchronizer;
    private final SplitClientContainer mClientContainer;
    private final String mDefaultKey;
    private String mLocalhostFileName = null;

    public LocalhostSplitFactory(String key, Context context, SplitClientConfig config) throws IOException {
        this(key, context, config, null);
    }

    public LocalhostSplitFactory(String key, Context context,
                                 SplitClientConfig config,
                                 String localhostFileName) throws IOException {

        if (localhostFileName != null) {
            mLocalhostFileName = localhostFileName;
        }

        mDefaultKey = key;
        EventsManagerCoordinator eventsManagerCoordinator = new EventsManagerCoordinator();
        FileStorage fileStorage = new FileStorage(context.getCacheDir(), ServiceConstants.LOCALHOST_FOLDER);
        SplitsStorage splitsStorage = new LocalhostSplitsStorage(mLocalhostFileName, context, fileStorage, eventsManagerCoordinator);
        SplitParser splitParser = new SplitParser(new LocalhostMySegmentsStorageContainer());
        SplitTaskExecutorImpl taskExecutor = new SplitTaskExecutorImpl();
        AttributesManagerFactory attributesManagerFactory = new AttributesManagerFactoryImpl(new AttributesValidatorImpl(), new ValidationMessageLoggerImpl());

        mManager = new SplitManagerImpl(splitsStorage, new SplitValidatorImpl(), splitParser);

        Set<String> configuredSets = new HashSet<>();
        if (config.syncConfig() != null) {
            List<SplitFilter> groupedFilters = new FilterBuilder(config.syncConfig().getFilters())
                    .getGroupedFilter();

            if (!groupedFilters.isEmpty() && groupedFilters.get(0).getType() == SplitFilter.Type.BY_SET) {
                configuredSets.addAll(groupedFilters.get(0).getValues());
            }
        }

        mClientContainer = new LocalhostSplitClientContainerImpl(this,
                config,
                splitsStorage,
                splitParser,
                attributesManagerFactory,
                new AttributesMergerImpl(),
                new NoOpTelemetryStorage(),
                eventsManagerCoordinator,
                taskExecutor,
                configuredSets);

        mSynchronizer = new LocalhostSynchronizer(taskExecutor, config, splitsStorage, buildQueryString(config.syncConfig()));
        mSynchronizer.start();

        Logger.i("Android SDK initialized!");
    }

    @VisibleForTesting
    LocalhostSplitFactory(@NonNull SplitsStorage splitsStorage,
                          @NonNull SplitParser splitParser,
                          @NonNull String defaultKey,
                          @NonNull LocalhostSynchronizer synchronizer,
                          @NonNull SplitClientContainer clientContainer) {

        mSynchronizer = synchronizer;
        mClientContainer = clientContainer;
        mDefaultKey = defaultKey;
        mManager = new SplitManagerImpl(splitsStorage, new SplitValidatorImpl(), splitParser);
    }

    @Override
    public SplitClient client() {
        return mClientContainer.getClient(new Key(mDefaultKey));
    }

    @Override
    public SplitClient client(Key key) {
        return mClientContainer.getClient(key);
    }

    @Override
    public SplitClient client(String matchingKey) {
        return mClientContainer.getClient(new Key(matchingKey));
    }

    @Override
    public SplitClient client(String matchingKey, String bucketingKey) {
        return mClientContainer.getClient(new Key(matchingKey, bucketingKey));
    }

    @Override
    public SplitManager manager() {
        return mManager;
    }

    @Override
    public void destroy() {
        mSynchronizer.stop();
    }

    @Override
    public void flush() {
        for (SplitClient client : mClientContainer.getAll()) {
            client.flush();
        }
    }

    @Override
    public void setUserConsent(boolean enabled) {
    }

    @Override
    public UserConsent getUserConsent() {
        return UserConsent.GRANTED;
    }

    private static String buildQueryString(SyncConfig syncConfig) {
        if (syncConfig != null) {
            FilterBuilder filterBuilder = new FilterBuilder(syncConfig.getFilters());
            return filterBuilder.buildQueryString();
        }

        return "";
    }
}
