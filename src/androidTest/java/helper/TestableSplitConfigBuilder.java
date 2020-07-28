package helper;

        import java.lang.reflect.Constructor;

        import io.split.android.client.ServiceEndpoints;
        import io.split.android.client.SplitClientConfig;
        import io.split.android.client.SyncConfig;
        import io.split.android.client.impressions.ImpressionListener;
        import io.split.android.client.utils.Logger;
        import okhttp3.Authenticator;

public class TestableSplitConfigBuilder {

    private ServiceEndpoints mServiceEndpoints = null;
    private int mFeaturesRefreshRate = 3600;
    private int mSegmentsRefreshRate = 1800;
    private int mImpressionsRefreshRate = 1800;
    private int mImpressionsQueueSize = 30000;
    private long mImpressionsChunkSize = 2 * 1024;
    private int mImpressionsPerPush = 10;
    private int mMetricsRefreshRate = 1800;
    private int mConnectionTimeout = 15000;
    private int mReadTimeout = 15000;
    private int mNumThreadsForSegmentFetch = 2;
    private int mReady = -1;
    private boolean mDebugEnabled = false;
    private boolean mLabelsEnabled = true;
    private ImpressionListener mImpressionListener;
    private int mWaitBeforeShutdown = 5000;
    private String mHostname;
    private String mIp;
    private String mProxy = null;
    private Authenticator mAuthenticator = null;
    private int mEventsQueueSize = 10000;
    private int mEventsPerPush = 2000;
    private long mEventFlushInterval = 1800;
    private String mTrafficType = null;
    private boolean mSynchronizeInBackground = false;
    private long mBackgroundSyncPeriod = 15;
    private boolean mBackgroundSyncWhenBatteryNotLow = true;
    private boolean mBackgroundSyncWhenWifiOnly = false;

    private boolean mStreamingEnabled = true;
    private int mAuthRetryBackoffBase = 1;
    private int mStreamingReconnectBackoffBase = 1;
    private boolean mEnableSslDevelopmentMode = false;
    private SyncConfig mSyncConfig;

    public TestableSplitConfigBuilder() {
        mServiceEndpoints = ServiceEndpoints.builder().build();
    }


    public TestableSplitConfigBuilder featuresRefreshRate(int featuresRefreshRate) {
        this.mFeaturesRefreshRate = featuresRefreshRate;
        return this;
    }

    public TestableSplitConfigBuilder segmentsRefreshRate(int segmentsRefreshRate) {
        this.mSegmentsRefreshRate = segmentsRefreshRate;
        return this;
    }

    public TestableSplitConfigBuilder impressionsRefreshRate(int impressionsRefreshRate) {
        this.mImpressionsRefreshRate = impressionsRefreshRate;
        return this;
    }

    public TestableSplitConfigBuilder impressionsQueueSize(int impressionsQueueSize) {
        this.mImpressionsQueueSize = impressionsQueueSize;
        return this;
    }

    public TestableSplitConfigBuilder impressionsChunkSize(long impressionsChunkSize) {
        this.mImpressionsChunkSize = impressionsChunkSize;
        return this;
    }

    public TestableSplitConfigBuilder metricsRefreshRate(int metricsRefreshRate) {
        this.mMetricsRefreshRate = metricsRefreshRate;
        return this;
    }

    public TestableSplitConfigBuilder connectionTimeout(int connectionTimeout) {
        this.mConnectionTimeout = connectionTimeout;
        return this;
    }

    public TestableSplitConfigBuilder readTimeout(int readTimeout) {
        this.mReadTimeout = readTimeout;
        return this;
    }

    public TestableSplitConfigBuilder numThreadsForSegmentFetch(int numThreadsForSegmentFetch) {
        this.mNumThreadsForSegmentFetch = numThreadsForSegmentFetch;
        return this;
    }

    public TestableSplitConfigBuilder ready(int ready) {
        this.mReady = ready;
        return this;
    }

    public TestableSplitConfigBuilder enableDebug() {
        this.mDebugEnabled = true;
        return this;
    }

    public TestableSplitConfigBuilder labelsEnabled(boolean labelsEnabled) {
        this.mLabelsEnabled = labelsEnabled;
        return this;
    }

    public TestableSplitConfigBuilder impressionListener(ImpressionListener impressionListener) {
        this.mImpressionListener = impressionListener;
        return this;
    }

    public TestableSplitConfigBuilder waitBeforeShutdown(int waitBeforeShutdown) {
        this.mWaitBeforeShutdown = waitBeforeShutdown;
        return this;
    }

    public TestableSplitConfigBuilder hostname(String hostname) {
        this.mHostname = hostname;
        return this;
    }

    public TestableSplitConfigBuilder ip(String ip) {
        this.mIp = ip;
        return this;
    }

    public TestableSplitConfigBuilder eventsQueueSize(int eventsQueueSize) {
        this.mEventsQueueSize = eventsQueueSize;
        return this;
    }

    public TestableSplitConfigBuilder eventsPerPush(int eventsPerPush) {
        this.mEventsPerPush = eventsPerPush;
        return this;
    }

    public TestableSplitConfigBuilder eventFlushInterval(long eventFlushInterval) {
        this.mEventFlushInterval = eventFlushInterval;
        return this;
    }

    public TestableSplitConfigBuilder trafficType(String trafficType) {
        this.mTrafficType = trafficType;
        return this;
    }

    public TestableSplitConfigBuilder synchronizeInBackground(boolean synchronizeInBackground) {
        this.mSynchronizeInBackground = synchronizeInBackground;
        return this;
    }

    public TestableSplitConfigBuilder impressionsPerPush(int impressionsPerPush) {
        this.mImpressionsPerPush = impressionsPerPush;
        return this;
    }

    public TestableSplitConfigBuilder streamingEnabled(boolean streamingEnabled) {
        mStreamingEnabled = streamingEnabled;
        return this;
    }

    public TestableSplitConfigBuilder authRetryBackoffBase(int authRetryBackoffBase) {
        mAuthRetryBackoffBase = authRetryBackoffBase;
        return this;
    }

    public TestableSplitConfigBuilder streamingReconnectBackoffBase(int streamingReconnectBackoffBase) {
        mStreamingReconnectBackoffBase = streamingReconnectBackoffBase;
        return this;
    }

    public TestableSplitConfigBuilder serviceEndpoints(ServiceEndpoints serviceEndpoints) {
        mServiceEndpoints = serviceEndpoints;
        return this;
    }

    public TestableSplitConfigBuilder enableSslDevelopmentMode() {
        mEnableSslDevelopmentMode = true;
        return this;
    }

    public TestableSplitConfigBuilder syncConfig(SyncConfig syncConfig) {
        mSyncConfig = syncConfig;
        return this;
    }

    public SplitClientConfig build() {
        Constructor constructor = SplitClientConfig.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        try {

            SplitClientConfig config = (SplitClientConfig) constructor.newInstance(
                    mServiceEndpoints.getSdkEndpoint(),
                    mServiceEndpoints.getEventsEndpoint(),
                    mFeaturesRefreshRate,
                    mSegmentsRefreshRate,
                    mImpressionsRefreshRate,
                    mImpressionsQueueSize,
                    mImpressionsChunkSize,
                    mImpressionsPerPush,
                    mMetricsRefreshRate,
                    mConnectionTimeout,
                    mReadTimeout,
                    mNumThreadsForSegmentFetch,
                    mReady,
                    mDebugEnabled,
                    mLabelsEnabled,
                    mImpressionListener,
                    mWaitBeforeShutdown,
                    mHostname,
                    mIp,
                    mProxy,
                    mAuthenticator,
                    mEventsQueueSize,
                    mEventsPerPush,
                    mEventFlushInterval,
                    mTrafficType,
                    mSynchronizeInBackground,
                    mBackgroundSyncPeriod,
                    mBackgroundSyncWhenBatteryNotLow,
                    mBackgroundSyncWhenWifiOnly,
                    mStreamingEnabled,
                    mAuthRetryBackoffBase,
                    mStreamingReconnectBackoffBase,
                    mServiceEndpoints.getAuthServiceEndpoint(),
                    mServiceEndpoints.getStreamingServiceEndpoint(),
                    mEnableSslDevelopmentMode,
                    mSyncConfig);
            return config;
        } catch (Exception e) {
            Logger.e("Error creating Testable Split client builder: "
                    + e.getLocalizedMessage());
        }
        return null;
    }
}

git add src/main/java/io/split/android/client/storage/splits/SplitsSnapshot.java
        git add  src/main/java/io/split/android/client/storage/splits/SplitsStorage.java
        git add  src/main/java/io/split/android/client/storage/splits/SplitsStorageImpl.java
        git add  src/main/java/io/split/android/client/storage/splits/SqLitePersistentSplitsStorage.java