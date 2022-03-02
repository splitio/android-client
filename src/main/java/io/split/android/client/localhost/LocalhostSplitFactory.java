package io.split.android.client.localhost;

import android.content.Context;

import java.io.IOException;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitManager;
import io.split.android.client.SplitManagerImpl;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesManagerFactory;
import io.split.android.client.attributes.AttributesManagerFactoryImpl;
import io.split.android.client.attributes.AttributesMergerImpl;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutorImpl;
import io.split.android.client.storage.attributes.AttributesStorageImpl;
import io.split.android.client.storage.legacy.FileStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.NoOpTelemetryStorage;
import io.split.android.client.utils.Logger;
import io.split.android.client.validators.AttributesValidatorImpl;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.client.validators.ValidationMessageLoggerImpl;
import io.split.android.engine.experiments.SplitParser;

/**
 * An implementation of SplitClient that considers all partitions
 * passed in the constructor to be 100% on for all users, and
 * any other split to be 100% off for all users. This implementation
 * is useful for using Codigo in localhost environment.
 *
 */
public final class LocalhostSplitFactory implements SplitFactory {

    private final SplitClient mClient;
    private final SplitManager mManager;
    private final SplitEventsManager mEventsManager;
    private final LocalhostSynchronizer mSynchronizer;
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

        mEventsManager = new SplitEventsManager(config);
        mEventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_LOADED_FROM_STORAGE);
        mEventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_FETCHED);
        mEventsManager.notifyInternalEvent(SplitInternalEvent.MY_SEGMENTS_UPDATED);
        FileStorage fileStorage = new FileStorage(context.getCacheDir(), ServiceConstants.LOCALHOST_FOLDER);
        SplitsStorage splitsStorage = new LocalhostSplitsStorage(mLocalhostFileName, context, fileStorage, mEventsManager);
        SplitParser splitParser = new SplitParser(new LocalhostMySegmentsStorageContainer());
        NoOpTelemetryStorage telemetryStorageProducer = new NoOpTelemetryStorage();
        SplitTaskExecutorImpl taskExecutor = new SplitTaskExecutorImpl();
        AttributesManagerFactory attributesManagerFactory = new AttributesManagerFactoryImpl(new AttributesValidatorImpl(), new ValidationMessageLoggerImpl());
        AttributesStorageImpl attributesStorage = new AttributesStorageImpl();
        AttributesManager attributesManager = attributesManagerFactory.getManager(key, attributesStorage);
        mClient = new LocalhostSplitClient(this, config, key, splitsStorage, mEventsManager, splitParser, attributesManager, new AttributesMergerImpl(), telemetryStorageProducer);
        mEventsManager.getExecutorResources().setSplitClient(mClient);
        mManager = new SplitManagerImpl(splitsStorage,
                new SplitValidatorImpl(), splitParser);
        mSynchronizer = new LocalhostSynchronizer(taskExecutor, config, splitsStorage);
        mSynchronizer.start();

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
        mSynchronizer.stop();
    }

    @Override
    public void flush() {
        mClient.flush();
    }

    @Override
    public boolean isReady() {
        return mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY);
    }
}
