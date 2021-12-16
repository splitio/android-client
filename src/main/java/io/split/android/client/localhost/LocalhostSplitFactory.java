package io.split.android.client.localhost;

import android.content.Context;

import java.io.IOException;
import java.util.List;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitManager;
import io.split.android.client.SplitManagerImpl;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesManagerImpl;
import io.split.android.client.attributes.AttributesMergerImpl;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskExecutorImpl;
import io.split.android.client.storage.attributes.AttributesStorageImpl;
import io.split.android.client.storage.legacy.FileStorage;
import io.split.android.client.storage.mysegments.EmptyMySegmentsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.model.EventsDataRecordsEnum;
import io.split.android.client.telemetry.model.HttpErrors;
import io.split.android.client.telemetry.model.HttpLatencies;
import io.split.android.client.telemetry.model.ImpressionsDataType;
import io.split.android.client.telemetry.model.LastSync;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.model.MethodExceptions;
import io.split.android.client.telemetry.model.MethodLatencies;
import io.split.android.client.telemetry.model.OperationType;
import io.split.android.client.telemetry.model.streaming.StreamingEvent;
import io.split.android.client.telemetry.storage.TelemetryStorage;
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
        SplitParser splitParser = new SplitParser(new EmptyMySegmentsStorage());
        SplitTaskExecutorImpl taskExecutor = new SplitTaskExecutorImpl();
        AttributesManager attributesManager = new AttributesManagerImpl(new AttributesStorageImpl(), new AttributesValidatorImpl(), new ValidationMessageLoggerImpl());
        mClient = new LocalhostSplitClient(this, config, key, splitsStorage, mEventsManager, splitParser, attributesManager, new AttributesMergerImpl(), new LocalhostTelemetryStorage());
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

    private class TelemetryStorageSub implements TelemetryStorage {

        @Override
        public MethodExceptions popExceptions() {
            return null;
        }

        @Override
        public MethodLatencies popLatencies() {
            return null;
        }

        @Override
        public void recordLatency(Method method, long latency) {

        }

        @Override
        public void recordException(Method method) {

        }

        @Override
        public long getNonReadyUsage() {
            return 0;
        }

        @Override
        public long getActiveFactories() {
            return 0;
        }

        @Override
        public long getRedundantFactories() {
            return 0;
        }

        @Override
        public long getTimeUntilReady() {
            return 0;
        }

        @Override
        public long getTimeUntilReadyFromCache() {
            return 0;
        }

        @Override
        public void recordNonReadyUsage() {

        }

        @Override
        public void recordActiveFactories(int count) {

        }

        @Override
        public void recordRedundantFactories(int count) {

        }

        @Override
        public void recordTimeUntilReady(long time) {

        }

        @Override
        public void recordTimeUntilReadyFromCache(long time) {

        }

        @Override
        public long getImpressionsStats(ImpressionsDataType type) {
            return 0;
        }

        @Override
        public long getEventsStats(EventsDataRecordsEnum type) {
            return 0;
        }

        @Override
        public LastSync getLastSynchronization() {
            return null;
        }

        @Override
        public HttpErrors popHttpErrors() {
            return null;
        }

        @Override
        public HttpLatencies popHttpLatencies() {
            return null;
        }

        @Override
        public long popAuthRejections() {
            return 0;
        }

        @Override
        public long popTokenRefreshes() {
            return 0;
        }

        @Override
        public List<StreamingEvent> popStreamingEvents() {
            return null;
        }

        @Override
        public List<String> popTags() {
            return null;
        }

        @Override
        public long getSessionLength() {
            return 0;
        }

        @Override
        public void addTag(String tag) {

        }

        @Override
        public void recordImpressionStats(ImpressionsDataType dataType, long count) {

        }

        @Override
        public void recordEventStats(EventsDataRecordsEnum dataType, long count) {

        }

        @Override
        public void recordSuccessfulSync(OperationType resource, long time) {

        }

        @Override
        public void recordSyncError(OperationType syncedResource, int status) {

        }

        @Override
        public void recordSyncLatency(OperationType resource, long latency) {

        }

        @Override
        public void recordAuthRejections() {

        }

        @Override
        public void recordTokenRefreshes() {

        }

        @Override
        public void recordStreamingEvents(StreamingEvent streamingEvent) {

        }

        @Override
        public void recordSessionLength(long sessionLength) {

        }
    }
}
