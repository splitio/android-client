package io.split.android.client;


import io.split.android.android_client.BuildConfig;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.utils.Logger;

/**
 * Configurations for the SplitClient.
 */
public class SplitClientConfig {

    private static final int MIN_FEATURES_REFRESH_RATE = 30;
    private static final int MIN_MYSEGMENTS_REFRESH_RATE = 30;
    private static final int MIN_IMPRESSIONS_REFRESH_RATE = 30;
    private static final int MIN_METRICS_REFRESH_RATE = 30;
    private static final int MIN_IMPRESSIONS_QUEUE_SIZE = 0;
    private static final int MIN_IMPRESSIONS_CHUNK_SIZE = 0;
    private static final int MIN_CONNECTION_TIMEOUT = 0;
    private static final int MIN_READ_TIMEOUT = 0;
    private static final int DEFAULT_FEATURES_REFRESH_RATE_SECS = 3600;
    private static final int DEFAULT_SEGMENTS_REFRESH_RATE_SECS = 1800;
    private static final int DEFAULT_IMPRESSIONS_REFRESH_RATE_SECS = 1800;
    private static final int DEFAULT_IMPRESSIONS_QUEUE_SIZE = 30000;
    private static final int DEFAULT_IMPRESSIONS_PER_PUSH = 2000;
    private static final int DEFAULT_CONNECTION_TIMEOUT_SECS = 15000;
    private static final int DEFAULT_READ_TIMEOUT_SECS = 15000;
    private static final int DEFAULT_NUM_THREAD_FOR_SEGMENT_FETCH = 2;
    private static final int DEFAULT_READY = -1;
    private static final int DEFAULT_METRICS_REFRESH_RATE_SECS = 1800;
    private static final int DEFAULT_WAIT_BEFORE_SHUTDOW_SECS = 5000;
    private static final int DEFAULT_IMPRESSIONS_CHUNK_SIZE = 2 * 1024;
    private static final int DEFAULT_EVENTS_QUEUE_SIZE = 10000;
    private static final int DEFAULT_EVENTS_FLUSH_INTERVAL = 1800;
    private static final int DEFAULT_EVENTS_PER_PUSH = 2000;
    private static final int DEFAULT_BACKGROUND_SYNC_PERIOD_MINUTES = 15;

    private static final int DEFAULT_AUTH_RETRY_BACKOFF_BASE_SECS = 1;
    private static final int DEFAULT_STREAMING_RECONNECT_BACKOFF_BASE_SECS = 1;

    private static final int IMPRESSIONS_MAX_SENT_ATTEMPTS = 3;
    private static final int IMPRESSIONS_CHUNK_OUTDATED_TIME = 3600 * 1000; // One day millis
    private final static int EVENTS_MAX_SENT_ATTEMPS = 3;
    private final static int MAX_QUEUE_SIZE_IN_BYTES = 5242880; // 5mb

    // Validation settings
    private static final int MAXIMUM_KEY_LENGTH = 250;
    private static final String TRACK_EVENT_NAME_PATTERN = "^[a-zA-Z0-9][-_.:a-zA-Z0-9]{0,79}$";

    // Data folder
    private static final String DEFAULT_DATA_FOLDER = "split_data";

    private String _endpoint;
    private String _eventsEndpoint;
    private static String _hostname;
    private static String _ip;

    private final int _featuresRefreshRate;
    private final int _segmentsRefreshRate;
    private final int _impressionsRefreshRate;
    private final int _impressionsQueueSize;
    private final int _impressionsPerPush;
    private final static int _impressionsMaxSentAttempts = IMPRESSIONS_MAX_SENT_ATTEMPTS;
    private final static long _impressionsChunkOudatedTime = IMPRESSIONS_CHUNK_OUTDATED_TIME;

    private final int _metricsRefreshRate;
    private final int _connectionTimeout;
    private final int _readTimeout;
    private final int _numThreadsForSegmentFetch;
    private final boolean _debugEnabled;
    private final boolean _labelsEnabled;
    private final int _ready;
    private final ImpressionListener _impressionListener;
    private final int _waitBeforeShutdown;
    private long _impressionsChunkSize;

    // Background sync
    private boolean _synchronizeInBackground;
    private long _backgroundSyncPeriod;
    private boolean _backgroundSyncWhenBatteryNotLow;
    private boolean _backgroundSyncWhenWifiOnly;

    //.Track configuration
    private final int _eventsQueueSize;
    private final int _eventsPerPush;
    private final long _eventFlushInterval;
    private final String _trafficType;

    // Push notification settings
    private boolean _streamingEnabled;
    private int _authRetryBackoffBase;
    private int _streamingReconnectBackoffBase;
    private String _authServiceUrl;
    private String _streamingServiceUrl;

    // To be set during startup
    public static String splitSdkVersion;

    public static Builder builder() {
        return new Builder();
    }


    private SplitClientConfig(String endpoint,
                              String eventsEndpoint,
                              int pollForFeatureChangesEveryNSeconds,
                              int segmentsRefreshRate,
                              int impressionsRefreshRate,
                              int impressionsQueueSize,
                              long impressionsChunkSize,
                              int impressionsPerPush,
                              int metricsRefreshRate,
                              int connectionTimeout,
                              int readTimeout,
                              int numThreadsForSegmentFetch,
                              int ready,
                              boolean debugEnabled,
                              boolean labelsEnabled,
                              ImpressionListener impressionListener,
                              int waitBeforeShutdown,
                              String hostname,
                              String ip,
                              int eventsQueueSize,
                              int eventsPerPush,
                              long eventFlushInterval,
                              String trafficType,
                              boolean synchronizeInBackground,
                              long backgroundSyncPeriod,
                              boolean backgroundSyncWhenBatteryNotLow,
                              boolean backgroundSyncWhenWifiOnly,
                              boolean streamingEnabled,
                              int authRetryBackoffBase,
                              int streamingReconnectBackoffBase,
                              String authServiceUrl,
                              String streamingServiceUrl) {
        _endpoint = endpoint;
        _eventsEndpoint = eventsEndpoint;
        _featuresRefreshRate = pollForFeatureChangesEveryNSeconds;
        _segmentsRefreshRate = segmentsRefreshRate;
        _impressionsRefreshRate = impressionsRefreshRate;
        _impressionsQueueSize = impressionsQueueSize;
        _impressionsPerPush = impressionsPerPush;
        _metricsRefreshRate = metricsRefreshRate;
        _connectionTimeout = connectionTimeout;
        _readTimeout = readTimeout;
        _numThreadsForSegmentFetch = numThreadsForSegmentFetch;
        _ready = ready;
        _debugEnabled = debugEnabled;
        _labelsEnabled = labelsEnabled;
        _impressionListener = impressionListener;
        _waitBeforeShutdown = waitBeforeShutdown;
        _impressionsChunkSize = impressionsChunkSize;
        _hostname = hostname;
        _ip = ip;

        _eventsQueueSize = eventsQueueSize;
        _eventsPerPush = eventsPerPush;
        _eventFlushInterval = eventFlushInterval;
        _trafficType = trafficType;
        _synchronizeInBackground = synchronizeInBackground;
        _backgroundSyncPeriod = backgroundSyncPeriod;
        _backgroundSyncWhenBatteryNotLow = backgroundSyncWhenBatteryNotLow;
        _backgroundSyncWhenWifiOnly = backgroundSyncWhenWifiOnly;
        _streamingEnabled = streamingEnabled;
        _authRetryBackoffBase = authRetryBackoffBase;
        _streamingReconnectBackoffBase = streamingReconnectBackoffBase;
        _authServiceUrl = authServiceUrl;
        _streamingServiceUrl = streamingServiceUrl;

        splitSdkVersion = "Android-" + BuildConfig.VERSION_NAME;

        if (_debugEnabled) {
            Logger.instance().debugLevel(true);
        }
    }

    private static boolean isTestMode() {
        boolean result;
        try {
            Class.forName("io.split.android.client.SplitClientConfigTest");
            result = true;
        } catch (final Exception e) {
            result = false;
        }
        return result;
    }

    public String trafficType() {
        return _trafficType;
    }

    public long eventFlushInterval() {
        return _eventFlushInterval;
    }

    public int eventsQueueSize() {
        return _eventsQueueSize;
    }

    public int eventsPerPush() {
        return _eventsPerPush;
    }

    public String endpoint() {
        return _endpoint;
    }

    public String eventsEndpoint() {
        return _eventsEndpoint;
    }

    public int featuresRefreshRate() {
        return _featuresRefreshRate;
    }

    public int segmentsRefreshRate() {
        return _segmentsRefreshRate;
    }

    public int numThreadsForSegmentFetch() {
        return _numThreadsForSegmentFetch;
    }

    public int impressionsRefreshRate() {
        return _impressionsRefreshRate;
    }

    public int impressionsQueueSize() {
        return _impressionsQueueSize;
    }

    public long impressionsChunkSize() {
        return _impressionsChunkSize;
    }

    public int impressionsPerPush() {
        return _impressionsPerPush;
    }

    public int metricsRefreshRate() {
        return _metricsRefreshRate;
    }

    public int connectionTimeout() {
        return _connectionTimeout;
    }

    public int readTimeout() {
        return _readTimeout;
    }

    public boolean debugEnabled() {
        return _debugEnabled;
    }

    public boolean labelsEnabled() {
        return _labelsEnabled;
    }

    public int blockUntilReady() {
        return _ready;
    }

    public ImpressionListener impressionListener() {
        return _impressionListener;
    }

    public int waitBeforeShutdown() {
        return _waitBeforeShutdown;
    }

    public String hostname() {
        return _hostname;
    }

    /**
     * Maximum attempts count while sending impressions.
     * to the server. Internal setting.
     *
     * @return Maximum attempts limit.
     */

    int impressionsMaxSentAttempts() {
        return _impressionsMaxSentAttempts;
    }

    /**
     * Elapsed time in millis to consider that a chunk of impression
     * is outdated. Internal property
     *
     * @return Time in millis.
     */
    long impressionsChunkOutdatedTime() {
        return _impressionsChunkOudatedTime;
    }

    /**
     * Maximum attempts count while sending tracks
     * to the server. Internal setting.
     *
     * @return Maximum attempts limit.
     */

    int eventsMaxSentAttempts() {
        return EVENTS_MAX_SENT_ATTEMPS;
    }

    /**
     * Maximum events queue size in bytes
     *
     * @return Maximum events queue size in bytes.
     */
    int maxQueueSizeInBytes() {
        return MAX_QUEUE_SIZE_IN_BYTES;
    }


    /**
     * Regex to validate Track event name
     *
     * @return Regex pattern string
     */
    String trackEventNamePattern() {
        return TRACK_EVENT_NAME_PATTERN;
    }


    /**
     * Maximum key char length for matching and bucketing
     *
     * @return Maximum char length
     */
    int maximumKeyLength() {
        return MAXIMUM_KEY_LENGTH;
    }

    /**
     * Default data folder to use when some
     * problem arises while creating it
     * based on api key
     *
     * @return Default data folder
     */
    String defaultDataFolder() {
        return DEFAULT_DATA_FOLDER;
    }

    public String ip() {
        return _ip;
    }

    public boolean synchronizeInBackground() {
        return _synchronizeInBackground;
    }

    public long backgroundSyncPeriod() {
        return _backgroundSyncPeriod;
    }

    public boolean backgroundSyncWhenBatteryNotLow() {
        return _backgroundSyncWhenBatteryNotLow;
    }

    public boolean backgroundSyncWhenBatteryWifiOnly() {
        return _backgroundSyncWhenWifiOnly;
    }

    // Push notification settings
    public boolean streamingEnabled() {
        return _streamingEnabled;
    }

    public int streamingReconnectBackoffBase() {
        return _streamingReconnectBackoffBase;
    }

    public String authServiceUrl() {
        return _authServiceUrl;
    }

    public String streamingServiceUrl() {
        return _streamingServiceUrl;
    }

    public static final class Builder {

        private ServiceEndpoints _serviceEndpoints = null;
        private int _featuresRefreshRate = DEFAULT_FEATURES_REFRESH_RATE_SECS;
        private int _segmentsRefreshRate = DEFAULT_SEGMENTS_REFRESH_RATE_SECS;
        private int _impressionsRefreshRate = DEFAULT_IMPRESSIONS_REFRESH_RATE_SECS;
        private int _impressionsQueueSize = DEFAULT_IMPRESSIONS_QUEUE_SIZE;
        private int _impressionsPerPush = DEFAULT_IMPRESSIONS_PER_PUSH;
        private int _connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_SECS;
        private int _readTimeout = DEFAULT_READ_TIMEOUT_SECS;
        private int _numThreadsForSegmentFetch = DEFAULT_NUM_THREAD_FOR_SEGMENT_FETCH;
        private boolean _debugEnabled = false;
        private int _ready = DEFAULT_READY; // -1 means no blocking
        private int _metricsRefreshRate = DEFAULT_METRICS_REFRESH_RATE_SECS;
        private boolean _labelsEnabled = true;
        private ImpressionListener _impressionListener;
        private int _waitBeforeShutdown = DEFAULT_WAIT_BEFORE_SHUTDOW_SECS;
        private long _impressionsChunkSize = DEFAULT_IMPRESSIONS_CHUNK_SIZE; //2KB default size

        //.track configuration
        private int _eventsQueueSize = DEFAULT_EVENTS_QUEUE_SIZE;
        private long _eventFlushInterval = DEFAULT_EVENTS_FLUSH_INTERVAL;
        private int _eventsPerPush = DEFAULT_EVENTS_PER_PUSH;
        private String _trafficType = null;

        private String _hostname = "unknown";
        private String _ip = "unknown";
        private boolean _synchronizeInBackground = false;
        private long _backgroundSyncPeriod = DEFAULT_BACKGROUND_SYNC_PERIOD_MINUTES;
        private boolean _backgroundSyncWhenBatteryNotLow = true;
        private boolean _backgroundSyncWhenWifiOnly = false;

        // Push notification settings
        private boolean _streamingEnabled = true;
        private int _authRetryBackoffBase = DEFAULT_AUTH_RETRY_BACKOFF_BASE_SECS;
        private int _streamingReconnectBackoffBase
                = DEFAULT_STREAMING_RECONNECT_BACKOFF_BASE_SECS;

        public Builder() {
            _serviceEndpoints = ServiceEndpoints.builder().build();
        }

        /**
         * Default Traffic Type to use in .track method
         *
         * @param trafficType
         * @return this builder
         */
        public Builder trafficType(String trafficType) {
            _trafficType = trafficType;
            return this;
        }

        /**
         * Max size of the queue to trigger a flush
         *
         * @param eventsQueueSize
         * @return this builder
         */
        public Builder eventsQueueSize(int eventsQueueSize) {
            _eventsQueueSize = eventsQueueSize;
            return this;
        }

        /**
         * Max size of the batch to push events
         *
         * @param eventsPerPush
         * @return this builder
         */
        public Builder eventsPerPush(int eventsPerPush) {
            _eventsPerPush = eventsPerPush;
            return this;
        }

        /**
         * How often to flush data to the collection services
         *
         * @param eventFlushInterval
         * @return this builder
         */
        public Builder eventFlushInterval(long eventFlushInterval) {
            _eventFlushInterval = eventFlushInterval;
            return this;
        }


        /**
         * The SDK will poll the endpoint for changes to features at this period.
         * <p>
         * Implementation Note: The SDK actually polls at a random interval
         * chosen between (0.5 * n, n). This is to ensure that
         * SDKs that are deployed simultaneously on different machines do not
         * inundate the backend with requests at the same interval.
         * </p>
         *
         * @param seconds MUST be greater than 0. Default value is 60.
         * @return this builder
         */
        public Builder featuresRefreshRate(int seconds) {
            _featuresRefreshRate = seconds;
            return this;
        }

        /**
         * The SDK will poll the endpoint for changes to segments at this period in seconds.
         * <p>
         * Implementation Note: The SDK actually polls at a random interval
         * chosen between (0.5 * n, n). This is to ensure that
         * SDKs that are deployed simultaneously on different machines do not
         * inundate the backend with requests at the same interval.
         * </p>
         *
         * @param seconds MUST be greater than 0. Default value is 60.
         * @return this builder
         */
        public Builder segmentsRefreshRate(int seconds) {
            _segmentsRefreshRate = seconds;
            return this;
        }

        /**
         * The ImpressionListener captures the which key saw what treatment ("on", "off", etc)
         * at what time. This log is periodically pushed back to split endpoint.
         * This parameter controls how quickly does the cache expire after a write.
         * <p/>
         * This is an ADVANCED parameter
         *
         * @param seconds MUST be > 0.
         * @return this builder
         */
        public Builder impressionsRefreshRate(int seconds) {
            _impressionsRefreshRate = seconds;
            return this;
        }

        /**
         * The impression listener captures the which key saw what treatment ("on", "off", etc)
         * at what time. This log is periodically pushed back to split endpoint.
         * This parameter controls the in-memory queue size to store them before they are
         * pushed back to split endpoint.
         * <p>
         * If the value chosen is too small and more than the default size(5000) of impressions
         * are generated, the old ones will be dropped and the sdk will show a warning.
         * <p/>
         * <p>
         * This is an ADVANCED parameter.
         *
         * @param impressionsQueueSize MUST be > 0. Default is 5000.
         * @return this builder
         */
        public Builder impressionsQueueSize(int impressionsQueueSize) {
            _impressionsQueueSize = impressionsQueueSize;
            return this;
        }

        /**
         * Max size of the batch to push impressions
         *
         * @param impressionsPerPush
         * @return this builder
         */
        public Builder impressionsPerPush(int impressionsPerPush) {
            _impressionsPerPush = impressionsPerPush;
            return this;
        }

        /**
         * You can provide your own ImpressionListener to capture all impressions
         * generated by SplitClient. An Impression is generated each time getTreatment is called.
         * <p>
         * <p>
         * Note that we will wrap any ImpressionListener provided in our own implementation
         * with an Executor controlling impressions going into your ImpressionListener. This is
         * done to protect SplitClient from any slowness caused by your ImpressionListener. The
         * Executor will be given the capacity you provide as parameter which is the
         * number of impressions that can be saved in a blocking queue while waiting for
         * your ImpressionListener to log them. Of course, the larger the value of capacity,
         * the more memory can be taken up.
         * <p>
         * <p>
         * The executor will create two threads.
         * <p>
         * <p>
         * This is an ADVANCED function.
         *
         * @param impressionListener
         * @return this builder
         */
        public Builder impressionListener(ImpressionListener impressionListener) {
            _impressionListener = impressionListener;
            return this;
        }

        /**
         * The diagnostic metrics collected by the SDK are pushed back to split endpoint
         * at this period.
         * <p/>
         * This is an ADVANCED parameter
         *
         * @param seconds MUST be > 0.
         * @return this builder
         */
        public Builder metricsRefreshRate(int seconds) {
            _metricsRefreshRate = seconds;
            return this;
        }

        /**
         * Http client connection timeout. Default value is 15000ms.
         *
         * @param ms MUST be greater than 0.
         * @return this builder
         */

        public Builder connectionTimeout(int ms) {
            _connectionTimeout = ms;
            return this;
        }

        /**
         * Http client read timeout. Default value is 15000ms.
         *
         * @param ms MUST be greater than 0.
         * @return this builder
         */
        public Builder readTimeout(int ms) {
            _readTimeout = ms;
            return this;
        }

        public Builder enableDebug() {
            _debugEnabled = true;
            return this;
        }

        /**
         * Disable label capturing
         *
         * @return this builder
         */
        public Builder disableLabels() {
            _labelsEnabled = false;
            return this;
        }


        /**
         * The SDK kicks off background threads to download data necessary
         * for using the SDK. You can choose to block until the SDK has
         * downloaded split definitions so that you will not get
         * the 'control' treatment.
         * <p/>
         * <p/>
         * If this parameter is set to a non-negative value, the SDK
         * will block for that number of milliseconds for the data to be downloaded.
         * <p/>
         * <p/>
         * If the download is not successful in this time period, a TimeOutException
         * will be thrown.
         * <p/>
         * <p/>
         * A negative value implies that the SDK building MUST NOT block. In this
         * scenario, the SDK might return the 'control' treatment until the
         * desired data has been downloaded.
         *
         * @param milliseconds MUST BE greater than or equal to 0;
         * @return this builder
         */
        public Builder ready(int milliseconds) {
            _ready = milliseconds;
            return this;
        }

        /**
         * How long to wait for impressions background thread before shutting down
         * the underlying connections.
         *
         * @param waitTime tine in milliseconds
         * @return this builder
         */
        public Builder waitBeforeShutdown(int waitTime) {
            _waitBeforeShutdown = waitTime;
            return this;
        }

        /**
         * Maximum size for impressions chunk to dump to storage and post.
         *
         * @param size MUST be > 0.
         * @return this builder
         */
        public Builder impressionsChunkSize(long size) {
            _impressionsChunkSize = size;
            return this;
        }

        /**
         * The host name for the current device.
         *
         * @param hostname
         * @return this builder
         */
        public Builder hostname(String hostname) {
            _hostname = hostname;
            return this;
        }

        /**
         * The current device IP adress.
         *
         * @param ip
         * @return this builder
         */
        public Builder ip(String ip) {
            _ip = ip;
            return this;
        }

        /**
         * When set to true app sync is done
         * using android resources event while app is in background.
         * Otherwise synchronization only occurs while app
         * is in foreground
         *
         * @return this builder
         */
        public Builder sychronizeInBackground(boolean synchronizeInBackground) {
            _synchronizeInBackground = synchronizeInBackground;
            return this;
        }

        /**
         * Period in minutes to execute background synchronization
         * Default values is 15 minutes and is the minimum allowed.
         * Is a lower value is especified default value will be used.
         *
         * @return this builder
         */
        public Builder sychronizeInBackgroundPeriod(long backgroundSyncPeriod) {
            _backgroundSyncPeriod = backgroundSyncPeriod;
            return this;
        }

        /**
         * Synchronize in background only if battery has no low charge level
         * Default value is set to true
         *
         * @return this builder
         */
        public Builder backgroundSyncWhenBatteryNotLow(boolean backgroundSyncWhenBatteryNotLow) {
            _backgroundSyncWhenBatteryNotLow = backgroundSyncWhenBatteryNotLow;
            return this;
        }

        /**
         * Synchronize in background only when a connection is wifi (unmetered)
         * When value is set to false, synchronization will occur whenever connection is available.
         * Default value is set to false
         *
         * @return this builder
         */
        public Builder backgroundSyncWhenWifiOnly(boolean backgroundSyncWhenWifiOnly) {
            _backgroundSyncWhenWifiOnly = backgroundSyncWhenWifiOnly;
            return this;
        }

        /**
         * Whether we should attempt to use streaming or not.
         * If the variable is false, the SDK will start in polling mode and stay that way.
         *
         * @return This builder
         * @default: True
         */
        public Builder streamingEnabled(boolean streamingEnabled) {
            _streamingEnabled = streamingEnabled;
            return this;
        }

        /**
         * How many seconds to wait before re attempting to authenticate for push notifications.
         * Minimum: 1 seconds
         *
         * @param authRetryBackoffBase
         * @return this builder
         * @default: 1 second
         */
        public Builder authRetryBackoffBase(int authRetryBackoffBase) {
            _authRetryBackoffBase = authRetryBackoffBase;
            return this;
        }

        /**
         * How many seconds to wait before re attempting to connect to streaming.
         *
         * @return: This builder
         * @default: 1 Second
         */
        public Builder streamingReconnectBackoffBase(int streamingReconnectBackoffBase) {
            _streamingReconnectBackoffBase = streamingReconnectBackoffBase;
            return this;
        }


        /**
         * Alternative service enpoints URL. Should only be adjusted for playing well in test environments.
         *
         * @param serviceEndpoints ServiceEndpoints
         * @return this builder
         */
        public Builder serviceEndpoints(ServiceEndpoints serviceEndpoints) {
            _serviceEndpoints = serviceEndpoints;
            return this;
        }

        public SplitClientConfig build() {


            if (_featuresRefreshRate < MIN_FEATURES_REFRESH_RATE) {
                throw new IllegalArgumentException("featuresRefreshRate must be >= 30: " + _featuresRefreshRate);
            }

            if (_segmentsRefreshRate < MIN_MYSEGMENTS_REFRESH_RATE) {
                throw new IllegalArgumentException("segmentsRefreshRate must be >= 30: " + _segmentsRefreshRate);
            }

            if (_impressionsRefreshRate < MIN_IMPRESSIONS_REFRESH_RATE) {
                throw new IllegalArgumentException("impressionsRefreshRate must be >= 30: " + _impressionsRefreshRate);
            }

            if (_metricsRefreshRate < MIN_METRICS_REFRESH_RATE) {
                throw new IllegalArgumentException("metricsRefreshRate must be >= 30: " + _metricsRefreshRate);
            }

            if (_impressionsQueueSize <= MIN_IMPRESSIONS_QUEUE_SIZE) {
                throw new IllegalArgumentException("impressionsQueueSize must be > 0: " + _impressionsQueueSize);
            }

            if (_impressionsChunkSize <= MIN_IMPRESSIONS_CHUNK_SIZE) {
                throw new IllegalArgumentException("impressionsChunkSize must be > 0: " + _impressionsChunkSize);
            }

            if (_connectionTimeout <= MIN_CONNECTION_TIMEOUT) {
                throw new IllegalArgumentException("connectionTimeOutInMs must be > 0: " + _connectionTimeout);
            }

            if (_readTimeout <= MIN_READ_TIMEOUT) {
                throw new IllegalArgumentException("readTimeout must be > 0: " + _readTimeout);
            }

            if (_numThreadsForSegmentFetch <= 0) {
                throw new IllegalArgumentException("Number of threads for fetching segments MUST be greater than zero");
            }

            if (_authRetryBackoffBase < 1) {
                throw new IllegalArgumentException("Re attempting time to authenticate " +
                        "for push notifications MUST be greater than zero");
            }

            if (_authRetryBackoffBase < 1) {
                throw new IllegalArgumentException("Re attempting time to connect to " +
                        "streaming notifications MUST be greater than zero");
            }

            if (_backgroundSyncPeriod < DEFAULT_BACKGROUND_SYNC_PERIOD_MINUTES) {
                Logger.w("Background sync period is lower than allowed. " +
                        "Setting to default value.");
                _backgroundSyncPeriod = DEFAULT_BACKGROUND_SYNC_PERIOD_MINUTES;
            }

            return new SplitClientConfig(
                    _serviceEndpoints.getSdkEndpoint(),
                    _serviceEndpoints.getEventsEndpoint(),
                    _featuresRefreshRate,
                    _segmentsRefreshRate,
                    _impressionsRefreshRate,
                    _impressionsQueueSize,
                    _impressionsChunkSize,
                    _impressionsPerPush,
                    _metricsRefreshRate,
                    _connectionTimeout,
                    _readTimeout,
                    _numThreadsForSegmentFetch,
                    _ready,
                    _debugEnabled,
                    _labelsEnabled,
                    _impressionListener,
                    _waitBeforeShutdown,
                    _hostname,
                    _ip,
                    _eventsQueueSize,
                    _eventsPerPush,
                    _eventFlushInterval,
                    _trafficType,
                    _synchronizeInBackground,
                    _backgroundSyncPeriod,
                    _backgroundSyncWhenBatteryNotLow,
                    _backgroundSyncWhenWifiOnly,
                    _streamingEnabled,
                    _authRetryBackoffBase,
                    _streamingReconnectBackoffBase,
                    _serviceEndpoints.getAuthServiceEndpoint(),
                    _serviceEndpoints.getStreamingServiceEndpoint());
        }

        public void set_impressionsChunkSize(long _impressionsChunkSize) {
            this._impressionsChunkSize = _impressionsChunkSize;
        }
    }
}