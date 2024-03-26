package io.split.android.client;


import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.net.URI;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import io.split.android.android_client.BuildConfig;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.network.DevelopmentSslConfig;
import io.split.android.client.network.HttpProxy;
import io.split.android.client.network.SplitAuthenticator;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.telemetry.TelemetryHelperImpl;
import io.split.android.client.utils.Utils;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;
import io.split.android.client.validators.PrefixValidatorImpl;
import io.split.android.client.validators.ValidationErrorInfo;

/**
 * Configurations for the SplitClient.
 */
public class SplitClientConfig {

    private static final int MIN_FEATURES_REFRESH_RATE = 30;
    private static final int MIN_MY_SEGMENTS_REFRESH_RATE = 30;
    private static final int MIN_IMPRESSIONS_REFRESH_RATE = 30;
    private static final int MIN_IMPRESSIONS_QUEUE_SIZE = 0;
    private static final int MIN_IMPRESSIONS_CHUNK_SIZE = 0;
    private static final int MIN_CONNECTION_TIMEOUT = 0;
    private static final int MIN_READ_TIMEOUT = 0;
    private static final int DEFAULT_FEATURES_REFRESH_RATE_SECS = 3600;
    private static final int DEFAULT_SEGMENTS_REFRESH_RATE_SECS = 1800;
    private static final int DEFAULT_IMPRESSIONS_REFRESH_RATE_SECS = 1800;
    private static final int DEFAULT_IMPRESSIONS_QUEUE_SIZE = 30000;
    private static final int DEFAULT_IMPRESSIONS_PER_PUSH = 2000;
    private static final int DEFAULT_IMP_COUNTERS_REFRESH_RATE_SECS = 1800;
    private static final int DEFAULT_CONNECTION_TIMEOUT_SECS = 10000;
    private static final int DEFAULT_READ_TIMEOUT_SECS = 10000;
    private static final int DEFAULT_READY = -1;
    private static final int DEFAULT_IMPRESSIONS_CHUNK_SIZE = 2 * 1024;
    private static final int DEFAULT_EVENTS_QUEUE_SIZE = 10000;
    private static final int DEFAULT_EVENTS_FLUSH_INTERVAL = 1800;
    private static final int DEFAULT_EVENTS_PER_PUSH = 2000;
    private static final int DEFAULT_BACKGROUND_SYNC_PERIOD_MINUTES = 15;

    private final static int DEFAULT_MTK_PER_PUSH = 30000;

    // Validation settings
    private static final int MAXIMUM_KEY_LENGTH = 250;
    private static final String TRACK_EVENT_NAME_PATTERN = "^[a-zA-Z0-9][-_.:a-zA-Z0-9]{0,79}$";

    // Data folder
    private static final String DEFAULT_DATA_FOLDER = "split_data";

    private static final long SPLITS_CACHE_EXPIRATION_IN_SECONDS = ServiceConstants.DEFAULT_SPLITS_CACHE_EXPIRATION_IN_SECONDS;
    private static final long OBSERVER_CACHE_EXPIRATION_PERIOD = ServiceConstants.DEFAULT_OBSERVER_CACHE_EXPIRATION_PERIOD_MS;

    private final String mEndpoint;
    private final String mEventsEndpoint;
    private final String mTelemetryEndpoint;
    private final String mHostname;
    private final String mIp;
    private final HttpProxy mProxy;
    private final SplitAuthenticator mProxyAuthenticator;

    private final int mFeaturesRefreshRate;
    private final int mSegmentsRefreshRate;
    private final int mImpressionsRefreshRate;
    private final int mImpressionsQueueSize;
    private final int mImpressionsPerPush;
    private final int mImpCountersRefreshRate;
    private final int mMtkPerPush;
    private final int mMtkRefreshRate;

    private final int mConnectionTimeout;
    private final int mReadTimeout;
    private final boolean mLabelsEnabled;
    private final int mReady;
    private final ImpressionListener mImpressionListener;
    private final long mImpressionsChunkSize;

    // Background sync
    private final boolean mSynchronizeInBackground;
    private final long mBackgroundSyncPeriod;
    private final boolean mBackgroundSyncWhenBatteryNotLow;
    private final boolean mBackgroundSyncWhenWifiOnly;

    //.Track configuration
    private final int mEventsQueueSize;
    private final int mEventsPerPush;
    private final long mEventFlushInterval;
    private final String mTrafficType;

    // Push notification settings
    private final boolean mStreamingEnabled;
    private final String mAuthServiceUrl;
    private final String mStreamingServiceUrl;
    private final DevelopmentSslConfig mDevelopmentSslConfig;

    private final SyncConfig mSyncConfig;

    private final boolean mLlegacyStorageMigrationEnabled;
    private final ImpressionsMode mImpressionsMode;
    private final boolean mIsPersistentAttributesEnabled;
    private final int mOfflineRefreshRate;
    private boolean mShouldRecordTelemetry;
    private final long mTelemetryRefreshRate;
    private boolean mSyncEnabled = true;
    private int mLogLevel = SplitLogLevel.NONE;
    private UserConsent mUserConsent;
    private boolean mEncryptionEnabled = false;
    private final String mPrefix;
    private final long mDefaultSSEConnectionDelayInSecs;
    private final long mSSEDisconnectionDelayInSecs;

    // To be set during startup
    public static String splitSdkVersion;
    private long mObserverCacheExpirationPeriod;

    public static Builder builder() {
        return new Builder();
    }

    private SplitClientConfig(String endpoint,
                              String eventsEndpoint,
                              int featureRefreshRate,
                              int segmentsRefreshRate,
                              int impressionsRefreshRate,
                              int impressionsQueueSize,
                              long impressionsChunkSize,
                              int impressionsPerPush,
                              int connectionTimeout,
                              int readTimeout,
                              int ready,
                              boolean labelsEnabled,
                              ImpressionListener impressionListener,
                              String hostname,
                              String ip,
                              HttpProxy proxy,
                              SplitAuthenticator proxyAuthenticator,
                              int eventsQueueSize,
                              int eventsPerPush,
                              long eventFlushInterval,
                              String trafficType,
                              boolean synchronizeInBackground,
                              long backgroundSyncPeriod,
                              boolean backgroundSyncWhenBatteryNotLow,
                              boolean backgroundSyncWhenWifiOnly,
                              boolean streamingEnabled,
                              String authServiceUrl,
                              String streamingServiceUrl,
                              DevelopmentSslConfig developmentSslConfig,
                              SyncConfig syncConfig,
                              boolean legacyStorageMigrationEnabled,
                              ImpressionsMode impressionsMode,
                              int impCountersRefreshRate,
                              boolean isPersistentAttributesEnabled,
                              int offlineRefreshRate,
                              String telemetryEndpoint,
                              long telemetryRefreshRate,
                              boolean shouldRecordTelemetry,
                              boolean syncEnabled,
                              int logLevel,
                              int mtkPerPush,
                              int mtkRefreshRate,
                              UserConsent userConsent,
                              boolean encryptionEnabled,
                              long defaultSSEConnectionDelayInSecs,
                              long sseDisconnectionDelayInSecs,
                              String prefix,
                              long observerCacheExpirationPeriod) {
        mEndpoint = endpoint;
        mEventsEndpoint = eventsEndpoint;
        mTelemetryEndpoint = telemetryEndpoint;
        mFeaturesRefreshRate = featureRefreshRate;
        mSegmentsRefreshRate = segmentsRefreshRate;
        mImpressionsRefreshRate = impressionsRefreshRate;
        mImpressionsQueueSize = impressionsQueueSize;
        mImpressionsPerPush = impressionsPerPush;
        mImpCountersRefreshRate = impCountersRefreshRate;
        mMtkRefreshRate = mtkRefreshRate;
        mConnectionTimeout = connectionTimeout;
        mReadTimeout = readTimeout;
        mReady = ready;
        mLabelsEnabled = labelsEnabled;
        mImpressionListener = impressionListener;
        mImpressionsChunkSize = impressionsChunkSize;
        mHostname = hostname;
        mIp = ip;

        mProxy = proxy;
        mProxyAuthenticator = proxyAuthenticator;

        mEventsQueueSize = eventsQueueSize;
        mEventsPerPush = eventsPerPush;
        mEventFlushInterval = eventFlushInterval;
        mTrafficType = trafficType;
        mSynchronizeInBackground = synchronizeInBackground;
        mBackgroundSyncPeriod = backgroundSyncPeriod;
        mBackgroundSyncWhenBatteryNotLow = backgroundSyncWhenBatteryNotLow;
        mBackgroundSyncWhenWifiOnly = backgroundSyncWhenWifiOnly;
        mStreamingEnabled = streamingEnabled;
        mAuthServiceUrl = authServiceUrl;
        mStreamingServiceUrl = streamingServiceUrl;
        mDevelopmentSslConfig = developmentSslConfig;
        mSyncConfig = syncConfig;
        mLlegacyStorageMigrationEnabled = legacyStorageMigrationEnabled;
        mImpressionsMode = impressionsMode;
        mIsPersistentAttributesEnabled = isPersistentAttributesEnabled;
        mOfflineRefreshRate = offlineRefreshRate;
        mTelemetryRefreshRate = telemetryRefreshRate;
        mSyncEnabled = syncEnabled;
        mLogLevel = logLevel;

        mUserConsent = userConsent;

        splitSdkVersion = "Android-" + BuildConfig.SPLIT_VERSION_NAME;

        mShouldRecordTelemetry = shouldRecordTelemetry;

        mMtkPerPush = mtkPerPush;
        mEncryptionEnabled = encryptionEnabled;
        mDefaultSSEConnectionDelayInSecs = defaultSSEConnectionDelayInSecs;
        mSSEDisconnectionDelayInSecs = sseDisconnectionDelayInSecs;
        mPrefix = prefix;
        mObserverCacheExpirationPeriod = observerCacheExpirationPeriod;
    }

    public String trafficType() {
        return mTrafficType;
    }

    public long cacheExpirationInSeconds() {
        return SPLITS_CACHE_EXPIRATION_IN_SECONDS;
    }

    public long eventFlushInterval() {
        return mEventFlushInterval;
    }

    public int eventsQueueSize() {
        return mEventsQueueSize;
    }

    public int eventsPerPush() {
        return mEventsPerPush;
    }

    public String endpoint() {
        return mEndpoint;
    }

    public String eventsEndpoint() {
        return mEventsEndpoint;
    }

    public String telemetryEndpoint() {
        return mTelemetryEndpoint;
    }

    public int featuresRefreshRate() {
        return mFeaturesRefreshRate;
    }

    public int segmentsRefreshRate() {
        return mSegmentsRefreshRate;
    }

    public int impressionsRefreshRate() {
        return mImpressionsRefreshRate;
    }

    public int impressionsQueueSize() {
        return mImpressionsQueueSize;
    }

    public long impressionsChunkSize() {
        return mImpressionsChunkSize;
    }

    public int impressionsPerPush() {
        return mImpressionsPerPush;
    }

    public int connectionTimeout() {
        return mConnectionTimeout;
    }

    public int readTimeout() {
        return mReadTimeout;
    }

    public boolean labelsEnabled() {
        return mLabelsEnabled;
    }

    public int blockUntilReady() {
        return mReady;
    }

    public ImpressionListener impressionListener() {
        return mImpressionListener;
    }

    public HttpProxy proxy() {
        return mProxy;
    }

    @Deprecated
    public SplitAuthenticator proxyAuthenticator() {
        return mProxyAuthenticator;
    }

    public String hostname() {
        return mHostname;
    }

    public int logLevel() {
        return mLogLevel;
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
     * based on SDK key
     * @return Default data folder
     */
    String defaultDataFolder() {
        return DEFAULT_DATA_FOLDER;
    }

    String prefix() {
        return mPrefix;
    }

    public String ip() {
        return mIp;
    }

    public boolean synchronizeInBackground() {
        return mSynchronizeInBackground;
    }

    public long backgroundSyncPeriod() {
        return mBackgroundSyncPeriod;
    }

    public boolean backgroundSyncWhenBatteryNotLow() {
        return mBackgroundSyncWhenBatteryNotLow;
    }

    public boolean backgroundSyncWhenBatteryWifiOnly() {
        return mBackgroundSyncWhenWifiOnly;
    }

    // Push notification settings
    public boolean streamingEnabled() {
        return mStreamingEnabled;
    }

    public String authServiceUrl() {
        return mAuthServiceUrl;
    }

    public String streamingServiceUrl() {
        return mStreamingServiceUrl;
    }

    public SplitAuthenticator authenticator() {
        return mProxyAuthenticator;
    }

    public DevelopmentSslConfig developmentSslConfig() {
        return mDevelopmentSslConfig;
    }

    public SyncConfig syncConfig() {
        return mSyncConfig;
    }

    public boolean isStorageMigrationEnabled() {
        return mLlegacyStorageMigrationEnabled;
    }

    public ImpressionsMode impressionsMode() {
        return mImpressionsMode;
    }

    public int impressionsCounterRefreshRate() {
        return mImpCountersRefreshRate;
    }

    public boolean persistentAttributesEnabled() {
        return mIsPersistentAttributesEnabled;
    }

    public int offlineRefreshRate() { return mOfflineRefreshRate; }

    public boolean shouldRecordTelemetry() {
        return mShouldRecordTelemetry;
    }

    public long telemetryRefreshRate() {
        return mTelemetryRefreshRate;
    }

    public boolean syncEnabled() { return mSyncEnabled; }

    public int mtkPerPush() {
        return mMtkPerPush;
    }

    public int mtkRefreshRate() {
        return mMtkRefreshRate;
    }

    public UserConsent userConsent() {
        return mUserConsent;
    }

    protected void setUserConsent(UserConsent status) {
        mUserConsent = status;
    }

    public boolean encryptionEnabled() {
        return mEncryptionEnabled;
    }

    public long defaultSSEConnectionDelay() {
        return mDefaultSSEConnectionDelayInSecs;
    }

    public long sseDisconnectionDelay() {
        return mSSEDisconnectionDelayInSecs;
    }

    private void enableTelemetry() { mShouldRecordTelemetry = true; }

    public long observerCacheExpirationPeriod() {
        return mObserverCacheExpirationPeriod;
    }

    public static final class Builder {

        static final int PROXY_PORT_DEFAULT = 80;
        private ServiceEndpoints mServiceEndpoints = null;
        private int mFeaturesRefreshRate = DEFAULT_FEATURES_REFRESH_RATE_SECS;
        private int mSegmentsRefreshRate = DEFAULT_SEGMENTS_REFRESH_RATE_SECS;
        private int mImpressionsRefreshRate = DEFAULT_IMPRESSIONS_REFRESH_RATE_SECS;
        private int mImpressionsQueueSize = DEFAULT_IMPRESSIONS_QUEUE_SIZE;
        private int mImpressionsPerPush = DEFAULT_IMPRESSIONS_PER_PUSH;
        private int mImpCountersRefreshRate = DEFAULT_IMP_COUNTERS_REFRESH_RATE_SECS;
        private int mConnectionTimeout = DEFAULT_CONNECTION_TIMEOUT_SECS;
        private int mReadTimeout = DEFAULT_READ_TIMEOUT_SECS;
        private int mReady = DEFAULT_READY; // -1 means no blocking
        private boolean mLabelsEnabled = true;
        private ImpressionListener mImpressionListener;
        private long mImpressionsChunkSize = DEFAULT_IMPRESSIONS_CHUNK_SIZE; //2KB default size
        private boolean mIsPersistentAttributesEnabled = false;
        static final int OFFLINE_REFRESH_RATE_DEFAULT = -1;
        static final int DEFAULT_TELEMETRY_REFRESH_RATE = 3600;

        //.track configuration
        private int mEventsQueueSize = DEFAULT_EVENTS_QUEUE_SIZE;
        private long mEventFlushInterval = DEFAULT_EVENTS_FLUSH_INTERVAL;
        private int mEventsPerPush = DEFAULT_EVENTS_PER_PUSH;
        private String mTrafficType = null;

        private String mHostname = "unknown";
        private String mIp = "unknown";

        private String mProxyHost = null;
        private SplitAuthenticator mProxyAuthenticator = null;

        private boolean mSynchronizeInBackground = false;
        private long mBackgroundSyncPeriod = DEFAULT_BACKGROUND_SYNC_PERIOD_MINUTES;
        private boolean mBackgroundSyncWhenBatteryNotLow = true;
        private boolean mBackgroundSyncWhenWifiOnly = false;

        // Push notification settings
        private boolean mStreamingEnabled = true;

        private DevelopmentSslConfig mDevelopmentSslConfig;

        private SyncConfig mSyncConfig = SyncConfig.builder().build();

        private boolean mLegacyStorageMigrationEnabled = false;

        private ImpressionsMode mImpressionsMode = ImpressionsMode.OPTIMIZED;

        private int mOfflineRefreshRate = OFFLINE_REFRESH_RATE_DEFAULT;

        private long mTelemetryRefreshRate = DEFAULT_TELEMETRY_REFRESH_RATE;

        private boolean mSyncEnabled = true;

        private int mLogLevel = SplitLogLevel.NONE;

        private final int mMtkPerPush = DEFAULT_MTK_PER_PUSH;

        private final int mMtkRefreshRate = 15 * 60;

        private UserConsent mUserConsent = UserConsent.GRANTED;

        private boolean mEncryptionEnabled = false;

        private final long mDefaultSSEConnectionDelayInSecs = ServiceConstants.DEFAULT_SSE_CONNECTION_DELAY_SECS;

        private final long mSSEDisconnectionDelayInSecs = 60L;

        private final long mObserverCacheExpirationPeriod = OBSERVER_CACHE_EXPIRATION_PERIOD;

        private String mPrefix = null;

        public Builder() {
            mServiceEndpoints = ServiceEndpoints.builder().build();
        }

        /**
         * Default Traffic Type to use in .track method
         *
         * @param trafficType
         * @return this builder
         */
        public Builder trafficType(String trafficType) {
            mTrafficType = trafficType;
            return this;
        }

        /**
         * Max size of the queue to trigger a flush
         *
         * @param eventsQueueSize
         * @return this builder
         */
        public Builder eventsQueueSize(int eventsQueueSize) {
            mEventsQueueSize = eventsQueueSize;
            return this;
        }

        /**
         * Max size of the batch to push events
         *
         * @param eventsPerPush
         * @return this builder
         */
        public Builder eventsPerPush(int eventsPerPush) {
            mEventsPerPush = eventsPerPush;
            return this;
        }

        /**
         * How often to flush data to the collection services
         *
         * @param eventFlushInterval
         * @return this builder
         */
        public Builder eventFlushInterval(long eventFlushInterval) {
            mEventFlushInterval = eventFlushInterval;
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
            mFeaturesRefreshRate = seconds;
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
            mSegmentsRefreshRate = seconds;
            return this;
        }

        /**
         * The ImpressionListener captures the key saw what treatment ("on", "off", etc)
         * at what time. This log is periodically pushed to Split.
         * This parameter controls how quickly the cache expires after a write.
         * <p/>
         * This is an ADVANCED parameter
         *
         * @param seconds MUST be > 0.
         * @return this builder
         */
        public Builder impressionsRefreshRate(int seconds) {
            mImpressionsRefreshRate = seconds;
            return this;
        }

        /**
         * The impression listener captures the which key saw what treatment ("on", "off", etc)
         * at what time. This log is periodically pushed to Split.
         * This parameter controls the in-memory queue size to store them before they are
         * pushed to Split.
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
            mImpressionsQueueSize = impressionsQueueSize;
            return this;
        }

        /**
         * Max size of the batch to push impressions
         *
         * @param impressionsPerPush
         * @return this builder
         */
        public Builder impressionsPerPush(int impressionsPerPush) {
            mImpressionsPerPush = impressionsPerPush;
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
            mImpressionListener = impressionListener;
            return this;
        }

        /**
         * Http client connection timeout. Default value is 10000ms.
         *
         * @param ms MUST be greater than 0.
         * @return this builder
         */
        public Builder connectionTimeout(int ms) {
            mConnectionTimeout = ms;
            return this;
        }

        /**
         * Http client read timeout. Default value is 10000ms.
         *
         * @param ms MUST be greater than 0.
         * @return this builder
         */
        public Builder readTimeout(int ms) {
            mReadTimeout = ms;
            return this;
        }

        /**
         * Level of logging.
         * The values are the same than standard Android logging plus NONE, to
         * disable logging. Any not supported value will be considered NONE.
         * {@link SplitLogLevel} or {@link android.util.Log} values can be used
         * @return this builder
         */
        public Builder logLevel(int level) {
            mLogLevel = level;
            return this;
        }

        /**
         * Disable label capturing
         *
         * @return this builder
         */
        public Builder disableLabels() {
            mLabelsEnabled = false;
            return this;
        }

        /**
         * The SDK kicks off background threads to download data necessary
         * for using the SDK. You can choose to block until the SDK has
         * downloaded feature flag definitions so that you will not get
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
            mReady = milliseconds;
            return this;
        }

        /**
         * The proxy URI in standard "scheme://user:password@domain:port/path format. Default is null.
         * If no port is provided default is 80
         *
         * @param proxyHost proxy URI
         * @return this builder
         */
        public Builder proxyHost(String proxyHost) {
            if (proxyHost != null && proxyHost.endsWith("/")) {
                mProxyHost = proxyHost.substring(0, proxyHost.length() - 1);
            } else {
                mProxyHost = proxyHost;
            }
            return this;
        }

        /**
         * Set a custom authenticator for the proxy. This feature is experimental and
         * and unsupported. It could be removed from the SDK
         *
         * @param proxyAuthenticator
         * @return this builder
         */
        public Builder proxyAuthenticator(SplitAuthenticator proxyAuthenticator) {
            mProxyAuthenticator = proxyAuthenticator;
            return this;
        }

        /**
         * Maximum size for impressions chunk to dump to storage and post.
         *
         * @param size MUST be > 0.
         * @return this builder
         */
        public Builder impressionsChunkSize(long size) {
            mImpressionsChunkSize = size;
            return this;
        }

        /**
         * The host name for the current device.
         *
         * @param hostname
         * @return this builder
         */
        public Builder hostname(String hostname) {
            mHostname = hostname;
            return this;
        }

        /**
         * The current device IP address.
         *
         * @param ip
         * @return this builder
         */
        public Builder ip(String ip) {
            mIp = ip;
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
        public Builder synchronizeInBackground(boolean synchronizeInBackground) {
            mSynchronizeInBackground = synchronizeInBackground;
            return this;
        }

        /**
         * Period in minutes to execute background synchronization.
         * Default value is 15 minutes and is the minimum allowed.
         * If a lower value is specified, the default value will be used.
         *
         * @return this builder
         */
        public Builder synchronizeInBackgroundPeriod(long backgroundSyncPeriod) {
            mBackgroundSyncPeriod = backgroundSyncPeriod;
            return this;
        }

        /**
         * Synchronize in background only if battery has no low charge level
         * Default value is set to true
         *
         * @return this builder
         */
        public Builder backgroundSyncWhenBatteryNotLow(boolean backgroundSyncWhenBatteryNotLow) {
            mBackgroundSyncWhenBatteryNotLow = backgroundSyncWhenBatteryNotLow;
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
            mBackgroundSyncWhenWifiOnly = backgroundSyncWhenWifiOnly;
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
            mStreamingEnabled = streamingEnabled;
            return this;
        }

        /**
         * Alternative service endpoints URL. Should only be adjusted for playing well in test environments.
         *
         * @param serviceEndpoints ServiceEndpoints
         * @return this builder
         */
        public Builder serviceEndpoints(ServiceEndpoints serviceEndpoints) {
            mServiceEndpoints = serviceEndpoints;
            return this;
        }

        /**
         * Allows setup a custom SSL factory and trust manager. Do not activate this feature in production.
         *
         * @return: This builder
         * @default: null
         */
        public Builder developmentSslConfig(@NonNull SSLSocketFactory sslSocketFactory,
                                            @NonNull X509TrustManager trustManager,
                                            @NonNull HostnameVerifier hostnameVerifier) {

            mDevelopmentSslConfig = new DevelopmentSslConfig(checkNotNull(sslSocketFactory),
                    checkNotNull(trustManager), checkNotNull(hostnameVerifier));
            return this;
        }

        /**
         * Settings to customize how data sync is done
         *
         * @return: This builder
         * @default: null
         */
        public Builder syncConfig(SyncConfig syncConfig) {
            mSyncConfig = syncConfig;
            return this;
        }

        /**
         * Activates migration from old storage to sqlite db
         *
         * @return: This builder
         * @default: false
         */
        public Builder legacyStorageMigrationEnabled(boolean legacyStorageMigrationEnabled) {
            mLegacyStorageMigrationEnabled = legacyStorageMigrationEnabled;
            return this;
        }

        /**
         * Setup the impressions mode.
         * @param mode Values:<br>
         *             DEBUG: All impressions are sent
         *             OPTIMIZED: Impressions are sent using an optimization algorithm
         *             NONE: Only unique keys evaluated for a particular feature flag are sent
         *
         * @return: This builder
         * @default: OPTIMIZED
         */
        public Builder impressionsMode(ImpressionsMode mode) {
            mImpressionsMode = mode;
            return this;
        }

        /**
         * Setup the impressions mode using a string.
         * @param mode Values:<br>
         *             DEBUG: All impressions are sent and
         *             OPTIMIZED: Impressions are sent using an optimization algorithm
         *             NONE: Only unique keys evaluated for a particular feature flag are sent
         *
         * <p>
         *  NOTE: If the string is invalid (Neither DEBUG, OPTIMIZED nor NONE) default value will be used
         *  </p>
         *
         * @return: This builder
         * @default: OPTIMIZED
         */
        public Builder impressionsMode(String mode) {
            mImpressionsMode = ImpressionsMode.fromString(mode);
            return this;
        }

        /**
         * Whether to enable persisting attributes.
         *
         * @return This builder
         */
        public Builder persistentAttributesEnabled(boolean enabled) {
            mIsPersistentAttributesEnabled = enabled;
            return this;
        }

        /**
         * Only used in localhost mode. If offlineRefreshRate is a positive integer, feature flag values
         * will be loaded from a local file every `offlineRefreshRate` seconds.
         *
         * @return: This builder
         * @default: -1 Second
         */
        public Builder offlineRefreshRate(int offlineRefreshRate) {
            mOfflineRefreshRate = offlineRefreshRate;
            return this;
        }

        /**
         * Rate in seconds for telemetry to be sent. Minimum value is 60 seconds.
         *
         * This is an ADVANCED parameter
         *
         * @param telemetryRefreshRate Rate in seconds for telemetry refresh.
         * @return This builder
         * @default 3600 seconds
         */
        public Builder telemetryRefreshRate(long telemetryRefreshRate) {
            mTelemetryRefreshRate = telemetryRefreshRate;
            return this;
        }

        /**
         * Sync data (default: true)
         *
         * @param syncEnabled if false, not streaming nor polling services are enabled, in which case the SDK has to be recreated to get latest definitions.
         */
        public Builder syncEnabled(boolean syncEnabled) {
            this.mSyncEnabled = syncEnabled;
            return this;
        }

        /**
         * User Consent
         * @param value Values:<br>
         *             GRANTED: Impressions and events are tracked and sent to the backend
         *             DECLINED: Impressions and events aren't tracked nor sent to the backend
         *             UNKNOWN: Impressions and events are tracked in memory and aren't sent to the backend
         *
         * @default GRANTED
         */
        public Builder userConsent(UserConsent value) {
            mUserConsent = value;
            Logger.v("User consent has been set to " + value.toString());
            return this;
        }

        /**
         * Enable/disable encryption of stored data.
         * @param enabled: Whether encryption is enabled or not.
         * @default: false
         * @return: This builder
         */
        public Builder encryptionEnabled(boolean enabled) {
            mEncryptionEnabled = enabled;
            return this;
        }

        /**
         * Optional prefix for the database name.
         * @param prefix Prefix for the database name.
         * @return This builder
         */
        public Builder prefix(String prefix) {
            mPrefix = (prefix == null) ? "" : prefix;
            return this;
        }

        public SplitClientConfig build() {
            Logger.instance().setLevel(mLogLevel);

            if (mFeaturesRefreshRate < MIN_FEATURES_REFRESH_RATE) {
                Logger.w("Features refresh rate is lower than allowed. " +
                        "Setting to default value.");
                mFeaturesRefreshRate = DEFAULT_FEATURES_REFRESH_RATE_SECS;
            }

            if (mSegmentsRefreshRate < MIN_MY_SEGMENTS_REFRESH_RATE) {
                Logger.w("Segments refresh rate is lower than allowed. " +
                        "Setting to default value.");
                mSegmentsRefreshRate = DEFAULT_SEGMENTS_REFRESH_RATE_SECS;
            }

            if (mImpressionsRefreshRate < MIN_IMPRESSIONS_REFRESH_RATE) {
                Logger.w("Impressions refresh rate is lower than allowed. " +
                        "Setting to default value.");
                mImpressionsRefreshRate = DEFAULT_IMPRESSIONS_REFRESH_RATE_SECS;
            }

            if (mImpressionsQueueSize <= MIN_IMPRESSIONS_QUEUE_SIZE) {
                Logger.w("Impressions queue size is lower than allowed. " +
                        "Setting to default value.");
                mImpressionsQueueSize = DEFAULT_IMPRESSIONS_QUEUE_SIZE;
            }

            if (mImpressionsChunkSize <= MIN_IMPRESSIONS_CHUNK_SIZE) {
                Logger.w("Impressions chunk size is lower than allowed. " +
                        "Setting to default value.");
                mImpressionsChunkSize = DEFAULT_IMPRESSIONS_CHUNK_SIZE;
            }

            if (mConnectionTimeout <= MIN_CONNECTION_TIMEOUT) {
                Logger.w("Connection timeout is lower than allowed. " +
                        "Setting to default value.");
                mConnectionTimeout = DEFAULT_CONNECTION_TIMEOUT_SECS;
            }

            if (mReadTimeout <= MIN_READ_TIMEOUT) {
                Logger.w("Read timeout is lower than allowed. " +
                        "Setting to default value.");
                mReadTimeout = DEFAULT_READ_TIMEOUT_SECS;
            }

            if (mBackgroundSyncPeriod < DEFAULT_BACKGROUND_SYNC_PERIOD_MINUTES) {
                Logger.w("Background sync period is lower than allowed. " +
                        "Setting to default value.");
                mBackgroundSyncPeriod = DEFAULT_BACKGROUND_SYNC_PERIOD_MINUTES;
            }

            if (mTelemetryRefreshRate < 60) {
                Logger.w("Telemetry refresh rate is lower than allowed. " +
                        "Setting to default value.");
                mTelemetryRefreshRate = DEFAULT_TELEMETRY_REFRESH_RATE;
            }

            if (mPrefix != null) {
                ValidationErrorInfo result = new PrefixValidatorImpl().validate(mPrefix);
                if (result != null) {
                    Logger.e(result.getErrorMessage());
                    Logger.w("Setting prefix to empty string");

                    mPrefix = "";
                }
            }

            HttpProxy proxy = parseProxyHost(mProxyHost);

            return new SplitClientConfig(
                    mServiceEndpoints.getSdkEndpoint(),
                    mServiceEndpoints.getEventsEndpoint(),
                    mFeaturesRefreshRate,
                    mSegmentsRefreshRate,
                    mImpressionsRefreshRate,
                    mImpressionsQueueSize,
                    mImpressionsChunkSize,
                    mImpressionsPerPush,
                    mConnectionTimeout,
                    mReadTimeout,
                    mReady,
                    mLabelsEnabled,
                    mImpressionListener,
                    mHostname,
                    mIp,
                    proxy,
                    mProxyAuthenticator,
                    mEventsQueueSize,
                    mEventsPerPush,
                    mEventFlushInterval,
                    mTrafficType,
                    mSynchronizeInBackground,
                    mBackgroundSyncPeriod,
                    mBackgroundSyncWhenBatteryNotLow,
                    mBackgroundSyncWhenWifiOnly,
                    mStreamingEnabled,
                    mServiceEndpoints.getAuthServiceEndpoint(),
                    mServiceEndpoints.getStreamingServiceEndpoint(),
                    mDevelopmentSslConfig,
                    mSyncConfig,
                    mLegacyStorageMigrationEnabled,
                    mImpressionsMode,
                    mImpCountersRefreshRate,
                    mIsPersistentAttributesEnabled,
                    mOfflineRefreshRate,
                    mServiceEndpoints.getTelemetryEndpoint(),
                    mTelemetryRefreshRate,
                    new TelemetryHelperImpl().shouldRecordTelemetry(),
                    mSyncEnabled,
                    mLogLevel,
                    mMtkPerPush,
                    mMtkRefreshRate,
                    mUserConsent,
                    mEncryptionEnabled,
                    mDefaultSSEConnectionDelayInSecs,
                    mSSEDisconnectionDelayInSecs,
                    mPrefix,
                    mObserverCacheExpirationPeriod);
        }

        private HttpProxy parseProxyHost(String proxyUri) {
            if (!Utils.isNullOrEmpty(proxyUri)) {
                try {
                    String username = null;
                    String password = null;
                    URI uri = URI.create(proxyUri);
                    int port = uri.getPort() != -1 ? uri.getPort() : PROXY_PORT_DEFAULT;
                    String userInfo = uri.getUserInfo();
                    if(!Utils.isNullOrEmpty(userInfo)) {
                        String[] userInfoComponents = userInfo.split(":");
                        if(userInfoComponents.length > 1) {
                            username = userInfoComponents[0];
                            password = userInfoComponents[1];
                        }
                    }
                    String host = String.format("%s%s", uri.getHost(), uri.getPath());
                    return new HttpProxy(host, port, username, password);
                } catch (IllegalArgumentException e) {
                    Logger.e("Proxy URI not valid: " + e.getLocalizedMessage());
                    throw new IllegalArgumentException();
                } catch (Exception e) {
                    Logger.e("Unknown error while parsing proxy URI: " + e.getLocalizedMessage());
                    throw new IllegalArgumentException();
                }
            }
            return null;
        }
    }
}
