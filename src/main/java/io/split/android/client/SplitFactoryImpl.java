package io.split.android.client;

import android.content.Context;

import androidx.work.WorkManager;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import io.split.android.client.api.Key;
import io.split.android.client.dtos.Event;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.MySegment;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.factory.FactoryMonitor;
import io.split.android.client.factory.FactoryMonitorImpl;
import io.split.android.client.impressions.IImpressionsStorage;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.impressions.ImpressionsManagerConfig;
import io.split.android.client.impressions.ImpressionsManagerImpl;
import io.split.android.client.lifecycle.LifecycleManager;
import io.split.android.client.metrics.CachedMetrics;
import io.split.android.client.metrics.FireAndForgetMetrics;
import io.split.android.client.metrics.HttpMetrics;
import io.split.android.client.network.HttpClient;
import io.split.android.client.network.HttpClientImpl;
import io.split.android.client.network.SdkTargetPath;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.HttpRequestBodySerializer;
import io.split.android.client.service.SplitApiFacade;
import io.split.android.client.service.SyncManager;
import io.split.android.client.service.SyncManagerImpl;
import io.split.android.client.service.events.EventsRequestBodySerializer;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.executor.SplitTaskExecutorImpl;
import io.split.android.client.service.executor.SplitTaskFactory;
import io.split.android.client.service.executor.SplitTaskFactoryImpl;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherImpl;
import io.split.android.client.service.http.HttpRecorder;
import io.split.android.client.service.http.HttpRecorderImpl;
import io.split.android.client.service.impressions.ImpressionsRequestBodySerializer;
import io.split.android.client.service.mysegments.MySegmentsResponseParser;
import io.split.android.client.service.splits.SplitChangeResponseParser;
import io.split.android.client.storage.SplitStorageContainer;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.events.PersistentEventsStorage;
import io.split.android.client.storage.events.SqLitePersistentEventsStorage;
import io.split.android.client.storage.impressions.PersistentImpressionsStorage;
import io.split.android.client.storage.impressions.SqLitePersistentImpressionsStorage;
import io.split.android.client.storage.legacy.FileStorage;
import io.split.android.client.storage.legacy.IStorage;
import io.split.android.client.storage.legacy.ImpressionsFileStorage;
import io.split.android.client.storage.legacy.ImpressionsStorageManager;
import io.split.android.client.storage.legacy.ImpressionsStorageManagerConfig;
import io.split.android.client.storage.legacy.TrackStorageManager;
import io.split.android.client.storage.mysegments.MySegmentsStorage;
import io.split.android.client.storage.mysegments.MySegmentsStorageImpl;
import io.split.android.client.storage.mysegments.PersistentMySegmentsStorage;
import io.split.android.client.storage.mysegments.SqLitePersistentMySegmentsStorage;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.storage.splits.SplitsStorageImpl;
import io.split.android.client.storage.splits.SqLitePersistentSplitsStorage;
import io.split.android.client.track.ITrackStorage;
import io.split.android.client.track.TrackClientConfig;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.NetworkHelper;
import io.split.android.client.utils.Utils;
import io.split.android.client.validators.ApiKeyValidator;
import io.split.android.client.validators.ApiKeyValidatorImpl;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.client.validators.ValidationConfig;
import io.split.android.client.validators.ValidationErrorInfo;
import io.split.android.client.validators.ValidationMessageLogger;
import io.split.android.client.validators.ValidationMessageLoggerImpl;
import io.split.android.engine.SDKReadinessGates;
import io.split.android.engine.experiments.SplitParser;
import io.split.android.engine.metrics.FetcherMetricsConfig;
import io.split.android.engine.metrics.Metrics;

public class SplitFactoryImpl implements SplitFactory {

    private static Random RANDOM = new Random();

    private final SplitClient _client;
    private final SplitManager _manager;
    private final Runnable destroyer;
    private final Runnable flusher;
    private boolean isTerminated = false;
    private final String _apiKey;


    private SDKReadinessGates gates;

    private TrackClient _trackClient;
    private FactoryMonitor _factoryMonitor = FactoryMonitorImpl.getSharedInstance();
    private LifecycleManager _lifecyleManager;
    private SyncManager _syncManager;
    private SplitTaskExecutor _splitTaskExecutor;

    public SplitFactoryImpl(String apiToken, Key key, SplitClientConfig config, Context context)
            throws URISyntaxException {

        ValidationConfig.getInstance().setMaximumKeyLength(config.maximumKeyLength());
        ValidationConfig.getInstance().setTrackEventNamePattern(config.trackEventNamePattern());
        ApiKeyValidator apiKeyValidator = new ApiKeyValidatorImpl();
        ValidationMessageLogger validationLogger = new ValidationMessageLoggerImpl();

        ValidationErrorInfo errorInfo = apiKeyValidator.validate(apiToken);
        String validationTag = "factory instantiation";
        if (errorInfo != null) {
            validationLogger.log(errorInfo, validationTag);
        }

        int factoryCount = _factoryMonitor.count(apiToken);
        if (factoryCount > 0) {
            validationLogger.w("You already have " + factoryCount + (factoryCount == 1 ? " factory" : " factories") + "with this API Key. We recommend keeping only " +
                    "one instance of the factory at all times (Singleton pattern) and reusing it throughout your application.", validationTag);
        } else if (_factoryMonitor.count() > 0) {
            validationLogger.w("You already have an instance of the Split factory. Make sure you definitely want this additional instance. We recommend " +
                    "keeping only one instance of the factory at all times (Singleton pattern) and reusing it throughout your application.", validationTag);
        }
        _factoryMonitor.add(apiToken);
        _apiKey = apiToken;

        SplitHttpHeadersBuilder headersBuilder = new SplitHttpHeadersBuilder();
        headersBuilder.setHostIp(config.ip());
        headersBuilder.setHostname(config.hostname());
        headersBuilder.setClientVersion(SplitClientConfig.splitSdkVersion);
        headersBuilder.setApiToken(apiToken);

        final HttpClient httpClient = new HttpClientImpl();
        httpClient.addHeaders(headersBuilder.build());

        URI rootTarget = URI.create(config.endpoint());
        URI eventsRootTarget = URI.create(config.eventsEndpoint());

        // TODO: 11/23/17  Add MetricsCache
        // Metrics
        HttpMetrics httpMetrics = HttpMetrics.create(httpClient, eventsRootTarget);
        final FireAndForgetMetrics uncachedFireAndForget = FireAndForgetMetrics.instance(httpMetrics, 2, 1000);

        SplitEventsManager _eventsManager = new SplitEventsManager(config);
        gates = new SDKReadinessGates();

        String dataFolderName = Utils.convertApiKeyToFolder(apiToken);
        if (dataFolderName == null) {
            dataFolderName = config.defaultDataFolder();
        }

        // TODO: On final implementation wrap this in a component
        SplitRoomDatabase splitRoomDatabase = SplitRoomDatabase.getDatabase(context, dataFolderName);
        PersistentMySegmentsStorage persistentMySegmentsStorage = new SqLitePersistentMySegmentsStorage(splitRoomDatabase, key.matchingKey());
        MySegmentsStorage mySegmentsStorage = new MySegmentsStorageImpl(persistentMySegmentsStorage);
        PersistentImpressionsStorage persistentImpressionsStorage = new SqLitePersistentImpressionsStorage(splitRoomDatabase, 100);
        PersistentEventsStorage persistentEventsStorage = new SqLitePersistentEventsStorage(splitRoomDatabase, 100);

        SplitParser splitParser = new SplitParser(mySegmentsStorage);


        // TODO: On final implementation wrap this in a component
        CachedMetrics cachedMetrics = new CachedMetrics(httpMetrics, TimeUnit.SECONDS.toMillis(config.metricsRefreshRate()));
        final FireAndForgetMetrics cachedFireAndForgetMetrics = FireAndForgetMetrics.instance(cachedMetrics, 2, 1000);

        NetworkHelper networkHelper = new NetworkHelper();

        FetcherMetricsConfig splitsfetcherMetricsConfig = new FetcherMetricsConfig(
                Metrics.SPLIT_CHANGES_FETCHER_EXCEPTION,
                Metrics.SPLIT_CHANGES_FETCHER_TIME,
                Metrics.SPLIT_CHANGES_FETCHER_STATUS
        );

        HttpFetcher<SplitChange> splitsFetcher = new HttpFetcherImpl<SplitChange>(httpClient,
                SdkTargetPath.splitChanges(config.endpoint()), cachedFireAndForgetMetrics,
                splitsfetcherMetricsConfig,
                networkHelper, new SplitChangeResponseParser());

        FetcherMetricsConfig mySegmentsfetcherMetricsConfig = new FetcherMetricsConfig(
                Metrics.MY_SEGMENTS_FETCHER_EXCEPTION,
                Metrics.MY_SEGMENTS_FETCHER_TIME,
                Metrics.MY_SEGMENTS_FETCHER_STATUS
        );

        HttpFetcher<List<MySegment>> mySegmentsFetcher = new HttpFetcherImpl<List<MySegment>>(httpClient,
                SdkTargetPath.mySegments(config.endpoint()), cachedFireAndForgetMetrics,
                mySegmentsfetcherMetricsConfig,
                networkHelper, new MySegmentsResponseParser());

        HttpRecorder<List<Event>> eventsRecorder = new HttpRecorderImpl<List<Event>>(
                httpClient, SdkTargetPath.events(config.eventsEndpoint()), networkHelper,
                new EventsRequestBodySerializer());

        HttpRecorder<List<KeyImpression>> impressionsRecorder = new HttpRecorderImpl<List<KeyImpression>>(
                httpClient, SdkTargetPath.events(config.eventsEndpoint()), networkHelper,
                new ImpressionsRequestBodySerializer());


        SplitApiFacade splitApiFacade = new SplitApiFacade(
                splitsFetcher, mySegmentsFetcher,
                eventsRecorder, impressionsRecorder);

        PersistentSplitsStorage persistentSplitsStorage = new SqLitePersistentSplitsStorage(splitRoomDatabase);
        SplitsStorage splitsStorage = new SplitsStorageImpl(persistentSplitsStorage);

        SplitTaskExecutor splitTaskExecutor = new SplitTaskExecutorImpl();

        SplitStorageContainer storageContainer = new SplitStorageContainer(
                splitsStorage, mySegmentsStorage,
                persistentEventsStorage, persistentImpressionsStorage);

        SplitTaskFactory splitTaskFactory = new SplitTaskFactoryImpl(config, splitApiFacade, storageContainer);

        _syncManager = new SyncManagerImpl(
                config, _splitTaskExecutor, storageContainer, splitTaskFactory,
                _eventsManager, WorkManager.getInstance(context)
        );


        // Impressionss
        ImpressionsStorageManagerConfig impressionsStorageManagerConfig = new ImpressionsStorageManagerConfig();
        impressionsStorageManagerConfig.setImpressionsMaxSentAttempts(config.impressionsMaxSentAttempts());
        impressionsStorageManagerConfig.setImpressionsChunkOudatedTime(config.impressionsChunkOutdatedTime());
        IImpressionsStorage impressionsStorage = new ImpressionsFileStorage(context.getCacheDir(), dataFolderName);
        final ImpressionsStorageManager impressionsStorageManager = new ImpressionsStorageManager(impressionsStorage, impressionsStorageManagerConfig);

        ImpressionsManagerConfig impressionsManagerConfig =
                new ImpressionsManagerConfig(config.impressionsChunkSize(),
                        config.waitBeforeShutdown(),
                        config.impressionsQueueSize(),
                        config.impressionsRefreshRate(), config.eventsEndpoint());
        final ImpressionsManagerImpl splitImpressionListener = ImpressionsManagerImpl.instance(httpClient, impressionsManagerConfig, impressionsStorageManager);
        final ImpressionListener impressionListener;

        if (config.impressionListener() != null) {
            List<ImpressionListener> impressionListeners = new ArrayList<ImpressionListener>();
            impressionListeners.add(splitImpressionListener);
            impressionListeners.add(config.impressionListener());
            impressionListener = new ImpressionListener.FederatedImpressionListener(impressionListeners);
        } else {
            impressionListener = splitImpressionListener;
        }

        TrackClientConfig trackConfig = new TrackClientConfig();
        trackConfig.setFlushIntervalMillis(config.eventFlushInterval());
        trackConfig.setMaxEventsPerPost(config.eventsPerPush());
        trackConfig.setMaxQueueSize(config.eventsQueueSize());
        trackConfig.setWaitBeforeShutdown(config.waitBeforeShutdown());
        trackConfig.setMaxSentAttempts(config.eventsMaxSentAttempts());
        trackConfig.setMaxQueueSizeInBytes(config.maxQueueSizeInBytes());
        ITrackStorage eventsStorage = new FileStorage.TracksFileStorage(context.getCacheDir(), dataFolderName);
        TrackStorageManager trackStorageManager = new TrackStorageManager(eventsStorage);
        _trackClient = TrackClientImpl.create(trackConfig, httpClient, eventsRootTarget, trackStorageManager);


        _lifecyleManager = new LifecycleManager(_syncManager, splitImpressionListener, _trackClient);

        destroyer = new Runnable() {
            public void run() {
                Logger.w("Shutdown called for split");
                try {
                    _lifecyleManager.destroy();
                    Logger.i("Successful shutdown of lifecycle manager");
                    _factoryMonitor.remove(_apiKey);
                    _trackClient.close();
                    Logger.i("Successful shutdown of segment fetchers");
                    uncachedFireAndForget.close();
                    Logger.i("Successful shutdown of metrics 1");
                    cachedFireAndForgetMetrics.close();
                    Logger.i("Successful shutdown of metrics 2");
                    impressionListener.close();
                    Logger.i("Successful shutdown of ImpressionListener");
                    httpClient.close();
                    Logger.i("Successful shutdown of httpclient");
                    _manager.destroy();
                    Logger.i("Successful shutdown of manager");

                } catch (Exception e) {
                    Logger.e(e, "We could not shutdown split");
                } finally {
                    isTerminated = true;
                }
            }
        };

        flusher = new Runnable() {
            @Override
            public void run() {
                Logger.w("Flush called for split");
                try {
                    _trackClient.flush();
                    Logger.i("Successful flush of track client");
                    splitImpressionListener.flushImpressions();
                    Logger.i("Successful flush of impressions");
                } catch (Exception e) {
                    Logger.e(e, "We could not flush split");
                }
            }
        };

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Using the full path to avoid conflicting with Thread.destroy()
                SplitFactoryImpl.this.destroy();
            }
        });


        _client = new SplitClientImpl(this, key, splitParser,
                impressionListener, cachedFireAndForgetMetrics, config, _eventsManager, splitsStorage, new EventPropertiesProcessorImpl());
        _manager = new SplitManagerImpl(splitsStorage, new SplitValidatorImpl(), splitParser);

        _eventsManager.getExecutorResources().setSplitClient(_client);

        boolean dataReady = true;
        Logger.i("Android SDK initialized!");
    }

    private static int findPollingPeriod(Random rand, int max) {
        int min = max / 2;
        return rand.nextInt((max - min) + 1) + min;
    }

    public SplitClient client() {
        return _client;
    }

    public SplitManager manager() {
        return _manager;
    }

    public void destroy() {
        synchronized (SplitFactoryImpl.class) {
            if (!isTerminated) {
                new Thread(destroyer).start();
            }
        }
    }

    @Override
    public void flush() {
        if (!isTerminated) {
            new Thread(flusher).start();
        }
    }

    @Override
    public boolean isReady() {
        return gates.isSDKReadyNow();
    }


}
