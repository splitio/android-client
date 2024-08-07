package io.split.android.client.telemetry.storage;

import static io.split.android.client.utils.Utils.checkNotNull;

import static io.split.android.client.ServiceEndpoints.EndpointValidator.authEndpointIsOverridden;
import static io.split.android.client.ServiceEndpoints.EndpointValidator.eventsEndpointIsOverridden;
import static io.split.android.client.ServiceEndpoints.EndpointValidator.sdkEndpointIsOverridden;
import static io.split.android.client.ServiceEndpoints.EndpointValidator.streamingEndpointIsOverridden;
import static io.split.android.client.ServiceEndpoints.EndpointValidator.telemetryEndpointIsOverridden;

import android.os.Build;

import androidx.annotation.NonNull;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.impressions.ImpressionsMode;
import io.split.android.client.telemetry.model.Config;
import io.split.android.client.telemetry.model.RefreshRates;
import io.split.android.client.telemetry.model.UrlOverrides;
import io.split.android.client.utils.logger.Logger;

public class TelemetryConfigProviderImpl implements TelemetryConfigProvider {

    private final TelemetryStorageConsumer mTelemetryConsumer;
    private final SplitClientConfig mSplitClientConfig;
    private final int mValidFlagSetCount;
    private final int mInvalidFlagSetCount;

    public TelemetryConfigProviderImpl(@NonNull TelemetryStorageConsumer telemetryConsumer,
                                       @NonNull SplitClientConfig splitClientConfig,
                                       int validFlagSetCount,
                                       int invalidFlagSetCount) {
        mTelemetryConsumer = checkNotNull(telemetryConsumer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
        mValidFlagSetCount = validFlagSetCount;
        mInvalidFlagSetCount = invalidFlagSetCount;
    }

    @Override
    public Config getConfigTelemetry() {
        Config config = new Config();
        addDefaultTags(mSplitClientConfig);
        config.setStreamingEnabled(mSplitClientConfig.streamingEnabled());
        config.setRefreshRates(buildRefreshRates(mSplitClientConfig));
        config.setTags(mTelemetryConsumer.popTags());
        config.setImpressionsListenerEnabled(mSplitClientConfig.impressionListener() != null);
        config.setTimeUntilSDKReady(mTelemetryConsumer.getTimeUntilReady());
        config.setTimeUntilSDKReadyFromCache(mTelemetryConsumer.getTimeUntilReadyFromCache());
        config.setRedundantActiveFactories(mTelemetryConsumer.getRedundantFactories());
        config.setActiveFactories(mTelemetryConsumer.getActiveFactories());
        config.setHttpProxyDetected(mSplitClientConfig.proxy() != null);
        config.setSDKNotReadyUsage(mTelemetryConsumer.getNonReadyUsage());
        config.setUrlOverrides(buildUrlOverrides(mSplitClientConfig));
        config.setImpressionsQueueSize(mSplitClientConfig.impressionsQueueSize());
        config.setEventsQueueSize(mSplitClientConfig.eventsQueueSize());
        config.setUserConsent(mSplitClientConfig.userConsent().intValue());
        config.setFlagSetsTotal(mValidFlagSetCount + mInvalidFlagSetCount);
        config.setFlagSetsInvalid(mInvalidFlagSetCount);
        config.setLargeSegmentsEnabled(mSplitClientConfig.largeSegmentsEnabled());
        config.setWaitForLargeSegments(mSplitClientConfig.waitForLargeSegments());
        if (mSplitClientConfig.impressionsMode() == ImpressionsMode.DEBUG) {
            config.setImpressionsMode(io.split.android.client.telemetry.model.ImpressionsMode.DEBUG.intValue());
        } else if (mSplitClientConfig.impressionsMode() == ImpressionsMode.OPTIMIZED) {
            config.setImpressionsMode(io.split.android.client.telemetry.model.ImpressionsMode.OPTIMIZED.intValue());
        } else {
            config.setImpressionsMode(io.split.android.client.telemetry.model.ImpressionsMode.NONE.intValue());
        }

        return config;
    }

    private RefreshRates buildRefreshRates(SplitClientConfig splitClientConfig) {
        RefreshRates refreshRates = new RefreshRates();
        refreshRates.setTelemetry(splitClientConfig.telemetryRefreshRate());
        refreshRates.setSplits(splitClientConfig.featuresRefreshRate());
        refreshRates.setMySegments(splitClientConfig.segmentsRefreshRate());
        refreshRates.setMyLargeSegments(splitClientConfig.largeSegmentsRefreshRate());
        refreshRates.setImpressions(splitClientConfig.impressionsRefreshRate());
        refreshRates.setEvents(splitClientConfig.eventFlushInterval());

        return refreshRates;
    }

    private UrlOverrides buildUrlOverrides(SplitClientConfig splitClientConfig) {
        UrlOverrides urlOverrides = new UrlOverrides();
        urlOverrides.setAuth(authEndpointIsOverridden(splitClientConfig.authServiceUrl()));
        urlOverrides.setSdkUrl(sdkEndpointIsOverridden(splitClientConfig.endpoint()));
        urlOverrides.setStream(streamingEndpointIsOverridden(splitClientConfig.streamingServiceUrl()));
        urlOverrides.setEvents(eventsEndpointIsOverridden(splitClientConfig.eventsEndpoint()));
        urlOverrides.setTelemetry(telemetryEndpointIsOverridden(splitClientConfig.telemetryEndpoint()));

        return urlOverrides;
    }

    private void addDefaultTags(SplitClientConfig mSplitClientConfig) {
        try {
            TelemetryRuntimeProducer producer = (TelemetryRuntimeProducer) mTelemetryConsumer;
            int sdkInt = Build.VERSION.SDK_INT;
            if (sdkInt > 0) {
                producer.addTag("av:" + sdkInt);
            }

            if (mSplitClientConfig.synchronizeInBackground()) {
                producer.addTag("bgr:" + mSplitClientConfig.backgroundSyncPeriod());
            }
        } catch (ClassCastException ex) {
            Logger.d("Telemetry storage is not a producer");
        }
    }
}
