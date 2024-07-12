package helper;

import java.lang.reflect.Constructor;

import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SyncConfig;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.network.CertificatePinningConfiguration;
import io.split.android.client.network.DevelopmentSslConfig;
import io.split.android.client.network.SplitAuthenticator;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.shared.UserConsent;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.logger.SplitLogLevel;

public class TestableSplitConfigBuilder {

    private ServiceEndpoints mServiceEndpoints = null;
    private int mFeaturesRefreshRate = 3600;
    private int mSegmentsRefreshRate = 1800;
    private int mImpressionsRefreshRate = 1800;
    private int mImpressionsQueueSize = 30000;
    private long mImpressionsChunkSize = 2 * 1024;
    private int mImpressionsPerPush = 10;
    private int mImpressionsCountersRefreshRate = 1800;
    private int mConnectionTimeout = 1500;
    private int mReadTimeout = 1500;
    private int mReady = -1;
    private boolean mLabelsEnabled = true;
    private ImpressionListener mImpressionListener;
    private String mHostname;
    private String mIp;
    private String mProxy = null;
    private SplitAuthenticator mAuthenticator = null;
    private int mEventsQueueSize = 10000;
    private int mEventsPerPush = 2000;
    private long mEventFlushInterval = 1800;
    private String mTrafficType = null;
    private boolean mSynchronizeInBackground = false;
    private long mBackgroundSyncPeriod = 15;
    private boolean mBackgroundSyncWhenBatteryNotLow = true;
    private boolean mBackgroundSyncWhenWifiOnly = false;
    private boolean mLegacyStorageMigrationEnabled = false;
    private boolean mIsPersistentAttributesStorageEnabled = false;
    private long mTelemetryRefreshRate = 3600;
    private boolean mShouldRecordTelemetry = false;

    private boolean mStreamingEnabled = true;
    private DevelopmentSslConfig mDevelopmentSslConfig = null;
    private ImpressionsMode mImpressionsMode = ImpressionsMode.OPTIMIZED;
    private SyncConfig mSyncConfig = SyncConfig.builder().build();
    private int mOfflineRefreshRate = 10;
    private boolean mSyncEnabled = true;
    private int mLogLevel = SplitLogLevel.NONE;
    private int mMtkPerPush = 30000;
    private int mMtkRefreshRate = 1800;
    private UserConsent mUserConsent = UserConsent.GRANTED;
    private boolean mEncryptionEnabled;
    private long mDefaultSSEConnectionDelayInSecs = ServiceConstants.DEFAULT_SSE_CONNECTION_DELAY_SECS;
    private long mSSEDisconnectionDelayInSecs = 60L;
    private long mObserverCacheExpirationPeriod = ServiceConstants.DEFAULT_OBSERVER_CACHE_EXPIRATION_PERIOD_MS;
    private String mPrefix = "";
    private CertificatePinningConfiguration mCertificatePinningConfiguration;
    private long mImpressionsDedupeTimeInterval = ServiceConstants.DEFAULT_IMPRESSIONS_DEDUPE_TIME_INTERVAL;
    private boolean mLargeSegmentsEnabled = false;
    private int mLargeSegmentsRefreshRate = 60;
    private boolean mWaitForLargeSegments = true;

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

    public TestableSplitConfigBuilder connectionTimeout(int connectionTimeout) {
        this.mConnectionTimeout = connectionTimeout;
        return this;
    }

    public TestableSplitConfigBuilder readTimeout(int readTimeout) {
        this.mReadTimeout = readTimeout;
        return this;
    }

    public TestableSplitConfigBuilder ready(int ready) {
        this.mReady = ready;
        return this;
    }

    public TestableSplitConfigBuilder enableDebug() {
        this.mLogLevel = SplitLogLevel.VERBOSE;
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

    public TestableSplitConfigBuilder serviceEndpoints(ServiceEndpoints serviceEndpoints) {
        mServiceEndpoints = serviceEndpoints;
        return this;
    }

    public TestableSplitConfigBuilder developmentSslConfig(DevelopmentSslConfig developmentSslConfig) {
        mDevelopmentSslConfig = developmentSslConfig;
        return this;
    }

    public TestableSplitConfigBuilder impressionsMode(ImpressionsMode mode) {
        mImpressionsMode = mode;
        return this;
    }

    public TestableSplitConfigBuilder syncConfig(SyncConfig syncConfig) {
        mSyncConfig = syncConfig;
        return this;
    }

    public TestableSplitConfigBuilder legacyStorageMigrationEnabled(boolean value) {
        mLegacyStorageMigrationEnabled = value;
        return this;
    }

    public TestableSplitConfigBuilder impressionsCountersRefreshRate(int impressionsCountersRefreshRate) {
        this.mImpressionsCountersRefreshRate = impressionsCountersRefreshRate;
        return this;
    }

    public TestableSplitConfigBuilder isPersistentAttributesStorageEnabled(boolean isPersistentAttributesStorageEnabled) {
        this.mIsPersistentAttributesStorageEnabled = isPersistentAttributesStorageEnabled;
        return this;
    }

    public TestableSplitConfigBuilder offlineRefreshRate(int offlineRefreshRate) {
        this.mOfflineRefreshRate = offlineRefreshRate;
        return this;
    }

    public TestableSplitConfigBuilder telemetryRefreshRate(long telemetryRefreshRate) {
        this.mTelemetryRefreshRate = telemetryRefreshRate;
        return this;
    }

    public TestableSplitConfigBuilder shouldRecordTelemetry(boolean shouldRecordTelemetry) {
        this.mShouldRecordTelemetry = shouldRecordTelemetry;
        return this;
    }

    public TestableSplitConfigBuilder syncEnabled(boolean syncEnabled) {
        this.mSyncEnabled = syncEnabled;
        return this;
    }

    public TestableSplitConfigBuilder mtkPerPush(int mtkPerPush) {
        this.mMtkPerPush = mtkPerPush;
        return this;
    }

    public TestableSplitConfigBuilder mtkRefreshRate(int mtkRefreshRate) {
        this.mMtkRefreshRate = mtkRefreshRate;
        return this;
    }

    public TestableSplitConfigBuilder userConsent(UserConsent value) {
        this.mUserConsent = value;
        return this;
    }

    public TestableSplitConfigBuilder encryptionEnabled(boolean enabled) {
        this.mEncryptionEnabled = enabled;
        return this;
    }

    public TestableSplitConfigBuilder defaultSSEConnectionDelayInSecs(long seconds) {
        this.mDefaultSSEConnectionDelayInSecs = seconds;
        return this;
    }

    public TestableSplitConfigBuilder sseDisconnectionDelayInSecs(long seconds) {
        this.mSSEDisconnectionDelayInSecs = seconds;
        return this;
    }

    public TestableSplitConfigBuilder prefix(String prefix) {
        this.mPrefix = prefix;
        return this;
    }

    public TestableSplitConfigBuilder observerCacheExpirationPeriod(long observerCacheExpirationPeriod) {
        this.mObserverCacheExpirationPeriod = observerCacheExpirationPeriod;
        return this;
    }

    public TestableSplitConfigBuilder certificatePinningConfiguration(CertificatePinningConfiguration certificatePinningConfiguration) {
        this.mCertificatePinningConfiguration = certificatePinningConfiguration;
        return this;
    }

    public TestableSplitConfigBuilder impressionsDedupeTimeInterval(long impressionsDedupeTimeInterval) {
        this.mImpressionsDedupeTimeInterval = impressionsDedupeTimeInterval;
        return this;
    }

    public TestableSplitConfigBuilder largeSegmentsEnabled(boolean largeSegmentsEnabled) {
        this.mLargeSegmentsEnabled = largeSegmentsEnabled;
        return this;
    }

    public TestableSplitConfigBuilder largeSegmentsRefreshRate(int largeSegmentsRefreshRate) {
        this.mLargeSegmentsRefreshRate = largeSegmentsRefreshRate;
        return this;
    }

    public TestableSplitConfigBuilder waitForLargeSegments(boolean waitForLargeSegments) {
        this.mWaitForLargeSegments = waitForLargeSegments;
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
                    mConnectionTimeout,
                    mReadTimeout,
                    mReady,
                    mLabelsEnabled,
                    mImpressionListener,
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
                    mServiceEndpoints.getAuthServiceEndpoint(),
                    mServiceEndpoints.getStreamingServiceEndpoint(),
                    mDevelopmentSslConfig,
                    mSyncConfig,
                    mLegacyStorageMigrationEnabled,
                    mImpressionsMode,
                    mImpressionsCountersRefreshRate,
                    mIsPersistentAttributesStorageEnabled,
                    mOfflineRefreshRate,
                    mServiceEndpoints.getTelemetryEndpoint(),
                    mTelemetryRefreshRate,
                    mShouldRecordTelemetry,
                    mSyncEnabled,
                    mLogLevel,
                    mMtkPerPush,
                    mMtkRefreshRate,
                    mUserConsent,
                    mEncryptionEnabled,
                    mDefaultSSEConnectionDelayInSecs,
                    mSSEDisconnectionDelayInSecs,
                    mPrefix,
                    mObserverCacheExpirationPeriod,
                    mCertificatePinningConfiguration,
                    mImpressionsDedupeTimeInterval,
                    mLargeSegmentsEnabled,
                    mLargeSegmentsRefreshRate,
                    mWaitForLargeSegments);

            Logger.instance().setLevel(mLogLevel);
            return config;
        } catch (Exception e) {
            Logger.e("Error creating Testable Split client builder: "
                    + e.getLocalizedMessage());
        }
        return null;
    }
}
