package io.split.android.client;


import androidx.annotation.NonNull;

import com.google.common.base.Strings;

import java.net.URI;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.X509TrustManager;

import io.split.android.android_client.BuildConfig;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.network.DevelopmentSslConfig;
import io.split.android.client.network.HttpProxy;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.telemetry.TelemetryHelperImpl;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;
import okhttp3.Authenticator;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Configurations for the SplitClient.
 */
public class SplitClientConfig {

    private static final int MIN_FEATURES_REFRESH_RATE = 30;
    private static final int MIN_MYSEGMENTS_REFRESH_RATE = 30;
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
    private static final int DEFAULT_CONNECTION_TIMEOUT_SECS = 15000;
    private static final int DEFAULT_READ_TIMEOUT_SECS = 15000;
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

    private static final long SPLITS_CACHE_EXPIRATION_IN_SECONDS = ServiceConstants.DEFAULT_SPLITS_CACHE_EXPIRATION_IN_SECONDS; // 10 días

    private final String _endpoint;
    private final String _eventsEndpoint;
    private final String _telemetryEndpoint;
    private final String _hostname;
    private final String _ip;
    private final HttpProxy _proxy;
    private final Authenticator _proxyAuthenticator;

    private final int _featuresRefreshRate;
    private final int _segmentsRefreshRate;
    private final int _impressionsRefreshRate;
    private final int _impressionsQueueSize;
    private final int _impressionsPerPush;
    private final int _impCountersRefreshRate;
    private final int _mtkPerPush;
    private final int _mtkRefreshRate;

    private final int _connectionTimeout;
    private final int _readTimeout;
    private final boolean _labelsEnabled;
    private final int _ready;
    private final ImpressionListener _impressionListener;
    private final long _impressionsChunkSize;

    // Background sync
    private final boolean _synchronizeInBackground;
    private final long _backgroundSyncPeriod;
    private final boolean _backgroundSyncWhenBatteryNotLow;
    private final boolean _backgroundSyncWhenWifiOnly;

    //.Track configuration
    private final int _eventsQueueSize;
    private final int _eventsPerPush;
    private final long _eventFlushInterval;
    private final String _trafficType;

    // Push notification settings
    private final boolean _streamingEnabled;
    private final String _authServiceUrl;
    private final String _streamingServiceUrl;
    private final DevelopmentSslConfig _developmentSslConfig;

    private final SyncConfig _syncConfig;

    private final boolean _legacyStorageMigrationEnabled;
    private final ImpressionsMode _impressionsMode;
    private final boolean _isPersistentAttributesEnabled;
    private final int _offlineRefreshRate;
    private boolean _shouldRecordTelemetry;
    private final long _telemetryRefreshRate;
    private boolean _syncEnabled = true;
    private int _logLevel = SplitLogLevel.NONE;
    private UserConsent _userConsent;

    // To be set during startup
    public static String splitSdkVersion;

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
                              Authenticator proxyAuthenticator,
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
                              UserConsent userConsent) {
        _endpoint = endpoint;
        _eventsEndpoint = eventsEndpoint;
        _telemetryEndpoint = telemetryEndpoint;
        _featuresRefreshRate = featureRefreshRate;
        _segmentsRefreshRate = segmentsRefreshRate;
        _impressionsRefreshRate = impressionsRefreshRate;
        _impressionsQueueSize = impressionsQueueSize;
        _impressionsPerPush = impressionsPerPush;
        _impCountersRefreshRate = impCountersRefreshRate;
        _mtkRefreshRate = mtkRefreshRate;
        _connectionTimeout = connectionTimeout;
        _readTimeout = readTimeout;
        _ready = ready;
        _labelsEnabled = labelsEnabled;
        _impressionListener = impressionListener;
        _impressionsChunkSize = impressionsChunkSize;
        _hostname = hostname;
        _ip = ip;

        _proxy = proxy;
        _proxyAuthenticator = proxyAuthenticator;

        _eventsQueueSize = eventsQueueSize;
        _eventsPerPush = eventsPerPush;
        _eventFlushInterval = eventFlushInterval;
        _trafficType = trafficType;
        _synchronizeInBackground = synchronizeInBackground;
        _backgroundSyncPeriod = backgroundSyncPeriod;
        _backgroundSyncWhenBatteryNotLow = backgroundSyncWhenBatteryNotLow;
        _backgroundSyncWhenWifiOnly = backgroundSyncWhenWifiOnly;
        _streamingEnabled = streamingEnabled;
        _authServiceUrl = authServiceUrl;
        _streamingServiceUrl = streamingServiceUrl;
        _developmentSslConfig = developmentSslConfig;
        _syncConfig = syncConfig;
        _legacyStorageMigrationEnabled = legacyStorageMigrationEnabled;
        _impressionsMode = impressionsMode;
        _isPersistentAttributesEnabled = isPersistentAttributesEnabled;
        _offlineRefreshRate = offlineRefreshRate;
        _telemetryRefreshRate = telemetryRefreshRate;
        _syncEnabled = syncEnabled;
        _logLevel = logLevel;
        _userConsent = userConsent;

        splitSdkVersion = "Android-" + BuildConfig.SPLIT_VERSION_NAME;

        _shouldRecordTelemetry = shouldRecordTelemetry;

        _mtkPerPush = mtkPerPush;

        Logger.instance().setLevel(_logLevel);
    }

    public String trafficType() {
        return _trafficType;
    }

    public long cacheExpirationInSeconds() {
        return SPLITS_CACHE_EXPIRATION_IN_SECONDS;
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

    public String telemetryEndpoint() {
        return _telemetryEndpoint;
    }

    public int featuresRefreshRate() {
        return _featuresRefreshRate;
    }

    public int segmentsRefreshRate() {
        return _segmentsRefreshRate;
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

    public int connectionTimeout() {
        return _connectionTimeout;
    }

    public int readTimeout() {
        return _readTimeout;
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

    public HttpProxy proxy() {
        return _proxy;
    }

    public Authenticator proxyAuthenticator() {
        return _proxyAuthenticator;
    }

    public String hostname() {
        return _hostname;
    }

    public int logLevel() {
        return _logLevel;
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

    public String authServiceUrl() {
        return _authServiceUrl;
    }

    public String streamingServiceUrl() {
        return _streamingServiceUrl;
    }

    public Authenticator authenticator() {
        return _proxyAuthenticator;
    }

    public DevelopmentSslConfig developmentSslConfig() {
        return _developmentSslConfig;
    }

    public SyncConfig syncConfig() {
        return _syncConfig;
    }

    public boolean isStorageMigrationEnabled() {
        return _legacyStorageMigrationEnabled;
    }

    public ImpressionsMode impressionsMode() {
        return _impressionsMode;
    }

    public int impressionsCounterRefreshRate() {
        return _impCountersRefreshRate;
    }

    public boolean persistentAttributesEnabled() {
        return _isPersistentAttributesEnabled;
    }

    public int offlineRefreshRate() { return  _offlineRefreshRate; }

    public boolean shouldRecordTelemetry() {
        return _shouldRecordTelemetry;
    }

    public long telemetryRefreshRate() {
        return _telemetryRefreshRate;
    }

    public boolean syncEnabled() { return _syncEnabled; }

    public int mtkPerPush() {
        return _mtkPerPush;
    }

    public int mtkRefreshRate() {
        return _mtkRefreshRate;
    }

    private void enableTelemetry() { _shouldRecordTelemetry = true; }

    public UserConsent userConsent() {
        return _userConsent;
    }

    protected void setUserConsent(UserConsent status) {
        _userConsent = status;
    }

    public static final class Builder {

        static final int PROXY_PORT_DEFAULT = 80;
        private ServiceEndpoints _serviceEndpoints = null;
        private int _featuresRefreshRate = DEFAULT_FEATURES_REFRESH_RATE_SECS;
        private int _segmentsRefreshRate = DEFAULT_SEGMENTS_REFRESH_RATE_SECS;
        private int _impressionsRefreshRate = DEFAULT_IMPRESSIONS_REFRESH_RATE_SECS;
        private int _impressionsQueueSize = DEFAULT_IMPRESSIONS_QUEUE_SIZE;
        private int _impressionsPerPush = DEFAULT_IMPRESSIONS_PER_PUSH;
        private int _impCountersRefreshRate = DEFAULT_IMP_COUNTERS_REFRESH_RATE_SECS;
        private int _connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_SECS;
        private int _readTimeout = DEFAULT_READ_TIMEOUT_SECS;
        private int _ready = DEFAULT_READY; // -1 means no blocking
        private boolean _labelsEnabled = true;
        private ImpressionListener _impressionListener;
        private long _impressionsChunkSize = DEFAULT_IMPRESSIONS_CHUNK_SIZE; //2KB default size
        private boolean _isPersistentAttributesEnabled = false;
        static final int OFFLINE_REFRESH_RATE_DEFAULT = -1;
        static final int DEFAULT_TELEMETRY_REFRESH_RATE = 3600;

        //.track configuration
        private int _eventsQueueSize = DEFAULT_EVENTS_QUEUE_SIZE;
        private long _eventFlushInterval = DEFAULT_EVENTS_FLUSH_INTERVAL;
        private int _eventsPerPush = DEFAULT_EVENTS_PER_PUSH;
        private String _trafficType = null;

        private String _hostname = "unknown";
        private String _ip = "unknown";

        private String _proxyHost = null;
        private Authenticator _proxyAuthenticator = null;

        private boolean _synchronizeInBackground = false;
        private long _backgroundSyncPeriod = DEFAULT_BACKGROUND_SYNC_PERIOD_MINUTES;
        private boolean _backgroundSyncWhenBatteryNotLow = true;
        private boolean _backgroundSyncWhenWifiOnly = false;

        // Push notification settings
        private boolean _streamingEnabled = true;

        private DevelopmentSslConfig _developmentSslConfig;

        private SyncConfig _syncConfig = SyncConfig.builder().build();

        private boolean _legacyStorageMigrationEnabled = false;

        private ImpressionsMode _impressionsMode = ImpressionsMode.OPTIMIZED;

        private int _offlineRefreshRate = OFFLINE_REFRESH_RATE_DEFAULT;

        private long _telemetryRefreshRate = DEFAULT_TELEMETRY_REFRESH_RATE;

        private boolean _syncEnabled = true;

        private int _logLevel = SplitLogLevel.NONE;

        private final int _mtkPerPush = DEFAULT_MTK_PER_PUSH;

        private UserConsent _userConsent = UserConsent.GRANTED;

        private final int _mtkRefreshRate = 15 * 60;

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

        /**
         * Level of logging.
         * The values are the same than standard Android logging plus NONE, to
         * disable logging. Any not supported value will be considered NONE.
         * {@link SplitLogLevel} or {@link android.util.Log} values can be used
         * @return this builder
         */
        public Builder logLevel(int level) {
            _logLevel = level;
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
         * The proxy URI in standard "scheme://user:password@domain:port/path format. Default is null.
         * If no port is provided default is 80
         *
         * @param proxyHost proxy URI
         * @return this builder
         */
        public Builder proxyHost(String proxyHost) {
            if (proxyHost != null && proxyHost.endsWith("/")) {
                _proxyHost = proxyHost.substring(0, proxyHost.length() - 1);
            } else {
                _proxyHost = proxyHost;
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
        public Builder proxyAuthenticator(Authenticator proxyAuthenticator) {
            _proxyAuthenticator = proxyAuthenticator;
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
         * The current device IP address.
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
        public Builder synchronizeInBackground(boolean synchronizeInBackground) {
            _synchronizeInBackground = synchronizeInBackground;
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
         * Alternative service endpoints URL. Should only be adjusted for playing well in test environments.
         *
         * @param serviceEndpoints ServiceEndpoints
         * @return this builder
         */
        public Builder serviceEndpoints(ServiceEndpoints serviceEndpoints) {
            _serviceEndpoints = serviceEndpoints;
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

            _developmentSslConfig = new DevelopmentSslConfig(checkNotNull(sslSocketFactory),
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
            _syncConfig = syncConfig;
            return this;
        }

        /**
         * Activates migration from old storage to sqlite db
         *
         * @return: This builder
         * @default: false
         */
        public Builder legacyStorageMigrationEnabled(boolean legacyStorageMigrationEnabled) {
            _legacyStorageMigrationEnabled = legacyStorageMigrationEnabled;
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
            _impressionsMode = mode;
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
            _impressionsMode = ImpressionsMode.fromString(mode);
            return this;
        }

        /**
         * Whether to enable persisting attributes.
         *
         * @return This builder
         */
        public Builder persistentAttributesEnabled(boolean enabled) {
            _isPersistentAttributesEnabled = enabled;
            return this;
        }

        /**
         * Only used in localhost mode. If offlineRefreshRate is a positive integer, split values
         * will be loaded from a local file every `offlineRefreshRate` seconds.
         *
         * @return: This builder
         * @default: -1 Second
         */
        public Builder offlineRefreshRate(int offlineRefreshRate) {
            _offlineRefreshRate = offlineRefreshRate;
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
            _telemetryRefreshRate = telemetryRefreshRate;
            return this;
        }

        /**
         * Sync data (default: true)
         *
         * @param syncEnabled if false, not streaming nor polling services are enabled, in which case the SDK has to be recreated to get latest definitions.
         */
        public Builder syncEnabled(boolean syncEnabled) {
            this._syncEnabled = syncEnabled;
            return this;
        }

        /**
         * User Consent
         * @param value Values:<br>
         *             GRANTED: Impressions and events are tracked and sent to the backend
         *             DECLINED: Impressions and events aren't tracked nor sent to the backend
         *             UNKNOWN: Impressions and events are tracked in memory and aren't sent to the backend
         *
         * @return: This builder
         * @default: GRANTED
         */
        public Builder userConsent(UserConsent value) {
            _userConsent = value;
            Logger.v("User consent has been set to " + value.toString());
            return this;
        }

        public SplitClientConfig build() {


            if (_featuresRefreshRate < MIN_FEATURES_REFRESH_RATE) {
                Logger.w("Features refresh rate is lower than allowed. " +
                        "Setting to default value.");
                _featuresRefreshRate = DEFAULT_FEATURES_REFRESH_RATE_SECS;
            }

            if (_segmentsRefreshRate < MIN_MYSEGMENTS_REFRESH_RATE) {
                Logger.w("Segments refresh rate is lower than allowed. " +
                        "Setting to default value.");
                _segmentsRefreshRate = DEFAULT_SEGMENTS_REFRESH_RATE_SECS;
            }

            if (_impressionsRefreshRate < MIN_IMPRESSIONS_REFRESH_RATE) {
                Logger.w("Impressions refresh rate is lower than allowed. " +
                        "Setting to default value.");
                _impressionsRefreshRate = DEFAULT_IMPRESSIONS_REFRESH_RATE_SECS;
            }

            if (_impressionsQueueSize <= MIN_IMPRESSIONS_QUEUE_SIZE) {
                Logger.w("Impressions queue size is lower than allowed. " +
                        "Setting to default value.");
                _impressionsQueueSize = DEFAULT_IMPRESSIONS_QUEUE_SIZE;
            }

            if (_impressionsChunkSize <= MIN_IMPRESSIONS_CHUNK_SIZE) {
                Logger.w("Impressions chunk size is lower than allowed. " +
                        "Setting to default value.");
                _impressionsChunkSize = DEFAULT_IMPRESSIONS_CHUNK_SIZE;
            }

            if (_connectionTimeout <= MIN_CONNECTION_TIMEOUT) {
                Logger.w("Connection timeout is lower than allowed. " +
                        "Setting to default value.");
                _connectionTimeout = DEFAULT_CONNECTION_TIMEOUT_SECS;
            }

            if (_readTimeout <= MIN_READ_TIMEOUT) {
                Logger.w("Read timeout is lower than allowed. " +
                        "Setting to default value.");
                _readTimeout = DEFAULT_READ_TIMEOUT_SECS;
            }

            if (_backgroundSyncPeriod < DEFAULT_BACKGROUND_SYNC_PERIOD_MINUTES) {
                Logger.w("Background sync period is lower than allowed. " +
                        "Setting to default value.");
                _backgroundSyncPeriod = DEFAULT_BACKGROUND_SYNC_PERIOD_MINUTES;
            }

            if (_telemetryRefreshRate < 60) {
                Logger.w("Telemetry refresh rate is lower than allowed. " +
                        "Setting to default value.");
                _telemetryRefreshRate = DEFAULT_TELEMETRY_REFRESH_RATE;
            }

            HttpProxy proxy = parseProxyHost(_proxyHost);

            return new SplitClientConfig(
                    _serviceEndpoints.getSdkEndpoint(),
                    _serviceEndpoints.getEventsEndpoint(),
                    _featuresRefreshRate,
                    _segmentsRefreshRate,
                    _impressionsRefreshRate,
                    _impressionsQueueSize,
                    _impressionsChunkSize,
                    _impressionsPerPush,
                    _connectionTimeout,
                    _readTimeout,
                    _ready,
                    _labelsEnabled,
                    _impressionListener,
                    _hostname,
                    _ip,
                    proxy,
                    _proxyAuthenticator,
                    _eventsQueueSize,
                    _eventsPerPush,
                    _eventFlushInterval,
                    _trafficType,
                    _synchronizeInBackground,
                    _backgroundSyncPeriod,
                    _backgroundSyncWhenBatteryNotLow,
                    _backgroundSyncWhenWifiOnly,
                    _streamingEnabled,
                    _serviceEndpoints.getAuthServiceEndpoint(),
                    _serviceEndpoints.getStreamingServiceEndpoint(),
                    _developmentSslConfig,
                    _syncConfig,
                    _legacyStorageMigrationEnabled,
                    _impressionsMode,
                    _impCountersRefreshRate,
                    _isPersistentAttributesEnabled,
                    _offlineRefreshRate,
                    _serviceEndpoints.getTelemetryEndpoint(),
                    _telemetryRefreshRate,
                    new TelemetryHelperImpl().shouldRecordTelemetry(),
                    _syncEnabled,
                    _logLevel,
                    _mtkPerPush,
                    _mtkRefreshRate,
                    _userConsent);
        }

        private HttpProxy parseProxyHost(String proxyUri) {
            if (!Strings.isNullOrEmpty(proxyUri)) {
                try {
                    String username = null;
                    String password = null;
                    URI uri = URI.create(proxyUri);
                    int port = uri.getPort() != -1 ? uri.getPort() : PROXY_PORT_DEFAULT;
                    String userInfo = uri.getUserInfo();
                    if(!Strings.isNullOrEmpty(userInfo)) {
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
