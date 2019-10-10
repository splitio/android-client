package helper;

        import java.lang.reflect.Constructor;

        import io.split.android.client.SplitClientConfig;
        import io.split.android.client.impressions.ImpressionListener;

public class TestableSplitConfigBuilder {

    private String mEndpoint = "https://sdk.split.io/api";
    private String mEventsEndpoint = "https://events.split.io/api";
    private int mFeaturesRefreshRate = 3600;
    private int mSegmentsRefreshRate = 1800;
    private int mImpressionsRefreshRate = 1800;
    private int mImpressionsQueueSize = 30000;
    private long mImpressionsChunkSize = 2 * 1024;
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
    private int mEventsQueueSize = 10000;
    private int mEventsPerPush = 2000;
    private long mEventFlushInterval = 1800;
    private String mTrafficType = null;

    public TestableSplitConfigBuilder endpoint(String endpoint, String eventsEndpoint) {
        this.mEndpoint = endpoint;
        this.mEventsEndpoint = eventsEndpoint;
        return this;
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

    public SplitClientConfig build() {
        Constructor constructor = SplitClientConfig.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        try {

            SplitClientConfig config = (SplitClientConfig) constructor.newInstance(
                    mEndpoint,
                    mEventsEndpoint,
                    mFeaturesRefreshRate,
                    mSegmentsRefreshRate,
                    mImpressionsRefreshRate,
                    mImpressionsQueueSize,
                    mImpressionsChunkSize,
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
                    mEventsQueueSize,
                    mEventsPerPush,
                    mEventFlushInterval,
                    mTrafficType);
            return config;
        } catch (Exception e) {
            System.out.println(e.getLocalizedMessage());
        }
        return null;
    }
}
