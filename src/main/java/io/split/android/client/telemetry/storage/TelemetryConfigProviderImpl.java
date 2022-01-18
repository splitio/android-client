package io.split.android.client.telemetry.storage;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.telemetry.model.Config;
import io.split.android.client.telemetry.model.RefreshRates;
import io.split.android.client.telemetry.model.UrlOverrides;

public class TelemetryConfigProviderImpl implements TelemetryConfigProvider {

    private final TelemetryStorageConsumer mTelemetryConsumer;
    private final SplitClientConfig mSplitClientConfig;

    public TelemetryConfigProviderImpl(@NonNull TelemetryStorageConsumer telemetryConsumer,
                                       @NonNull SplitClientConfig splitClientConfig) {
        mTelemetryConsumer = checkNotNull(telemetryConsumer);
        mSplitClientConfig = checkNotNull(splitClientConfig);
    }

    @Override
    public Config getConfigTelemetry() {
        Config config = new Config();
        RefreshRates refreshRates = new RefreshRates();
        refreshRates.setTelemetry(mSplitClientConfig.telemetryRefreshRate());
        refreshRates.setSplits(mSplitClientConfig.featuresRefreshRate());
        refreshRates.setMySegments(mSplitClientConfig.segmentsRefreshRate());

        refreshRates.setImpressions(mSplitClientConfig.impressionsRefreshRate());

        config.setStreamingEnabled(mSplitClientConfig.streamingEnabled());
        config.setRefreshRates(refreshRates);
        config.setTags(mTelemetryConsumer.popTags());
        config.setImpressionsListenerEnabled(mSplitClientConfig.impressionListener() != null);
        config.setTimeUntilSDKReady(mTelemetryConsumer.getTimeUntilReady());
        config.setTimeUntilSDKReadyFromCache(mTelemetryConsumer.getTimeUntilReadyFromCache());
        config.setRedundantActiveFactories(mTelemetryConsumer.getRedundantFactories());
        config.setActiveFactories(mTelemetryConsumer.getActiveFactories());
        config.setHttpProxyDetected(mSplitClientConfig.proxy() != null);
        config.setSDKNotReadyUsage(mTelemetryConsumer.getNonReadyUsage());

        return config;
    }
}
