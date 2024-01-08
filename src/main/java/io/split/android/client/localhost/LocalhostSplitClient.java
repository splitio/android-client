package io.split.android.client.localhost;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.EvaluatorImpl;
import io.split.android.client.FlagSetsFilter;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitResult;
import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.shared.SplitClientContainer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.validators.KeyValidatorImpl;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.client.validators.TreatmentManager;
import io.split.android.client.validators.TreatmentManagerImpl;
import io.split.android.client.validators.ValidationMessageLoggerImpl;
import io.split.android.engine.experiments.SplitParser;
import io.split.android.grammar.Treatments;

/**
 * An implementation of SplitClient that considers all partitions
 * passed in the constructor to be 100% on for all users, and
 * any other split to be 100% off for all users. This implementation
 * is useful for using in localhost environment.
 */
public final class LocalhostSplitClient implements SplitClient {
    private final WeakReference<LocalhostSplitFactory> mFactoryRef;
    private final WeakReference<SplitClientContainer> mClientContainer;
    private final Key mKey;
    private final SplitEventsManager mEventsManager;
    private final TreatmentManager mTreatmentManager;
    private boolean mIsClientDestroyed = false;
    private final SplitsStorage mSplitsStorage;

    public LocalhostSplitClient(@NonNull LocalhostSplitFactory container,
                                @NonNull SplitClientContainer clientContainer,
                                @NonNull SplitClientConfig splitClientConfig,
                                @NonNull Key key,
                                @NonNull SplitsStorage splitsStorage,
                                @NonNull SplitEventsManager eventsManager,
                                @NonNull SplitParser splitParser,
                                @NonNull AttributesManager attributesManager,
                                @NonNull AttributesMerger attributesMerger,
                                @NonNull TelemetryStorageProducer telemetryStorageProducer,
                                @Nullable FlagSetsFilter flagSetsFilter) {

        mFactoryRef = new WeakReference<>(checkNotNull(container));
        mClientContainer = new WeakReference<>(checkNotNull(clientContainer));
        mKey = checkNotNull(key);
        mEventsManager = checkNotNull(eventsManager);
        mSplitsStorage = splitsStorage;
        mTreatmentManager = new TreatmentManagerImpl(mKey.matchingKey(), mKey.bucketingKey(),
                new EvaluatorImpl(splitsStorage, splitParser), new KeyValidatorImpl(),
                new SplitValidatorImpl(), getImpressionsListener(splitClientConfig),
                splitClientConfig.labelsEnabled(), eventsManager, attributesManager, attributesMerger,
                telemetryStorageProducer, flagSetsFilter, splitsStorage, new ValidationMessageLoggerImpl());
    }

    @Override
    public String getTreatment(String featureFlagName) {
        try {
            return mTreatmentManager.getTreatment(featureFlagName, null, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e(exception);

            return Treatments.CONTROL;
        }
    }

    @Override
    public String getTreatment(String featureFlagName, Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatment(featureFlagName, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e(exception);

            return Treatments.CONTROL;
        }
    }

    @Override
    public SplitResult getTreatmentWithConfig(String featureFlagName, Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatmentWithConfig(featureFlagName, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e(exception);

            return new SplitResult(Treatments.CONTROL);
        }
    }

    @Override
    public Map<String, String> getTreatments(List<String> featureFlagNames, Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatments(featureFlagNames, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e(exception);

            Map<String, String> result = new HashMap<>();

            for (String featureFlagName : featureFlagNames) {
                result.put(featureFlagName, Treatments.CONTROL);
            }

            return result;
        }
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> featureFlagNames, Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatmentsWithConfig(featureFlagNames, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e(exception);

            Map<String, SplitResult> result = new HashMap<>();

            for (String featureFlagName : featureFlagNames) {
                result.put(featureFlagName, new SplitResult(Treatments.CONTROL));
            }

            return result;
        }
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatmentsByFlagSet(flagSet, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e(exception);

            return buildExceptionResult(Collections.singletonList(flagSet));
        }
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatmentsByFlagSets(flagSets, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e(exception);

            return buildExceptionResult(flagSets);
        }
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatmentsWithConfigByFlagSet(flagSet, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e(exception);

            return buildExceptionResultWithConfig(Collections.singletonList(flagSet));
        }
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatmentsWithConfigByFlagSets(flagSets, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e(exception);

            return buildExceptionResultWithConfig(flagSets);
        }
    }

    @Override
    public void destroy() {
        mIsClientDestroyed = true;
        SplitClientContainer splitClientContainer = mClientContainer.get();
        if (splitClientContainer != null) {
            splitClientContainer.remove(mKey);
        }

        SplitFactory factory = mFactoryRef.get();
        if (factory != null) {
            factory.destroy();
        }
    }

    @Override
    public void flush() {
    }

    @Override
    public boolean isReady() {
        return mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY);
    }

    public void on(SplitEvent event, SplitEventTask task) {
        checkNotNull(event);
        checkNotNull(task);

        if (!event.equals(SplitEvent.SDK_READY_FROM_CACHE) && mEventsManager.eventAlreadyTriggered(event)) {
            Logger.w(String.format("A listener was added for %s on the SDK, which has already fired and won’t be emitted again. The callback won’t be executed.", event.toString()));
            return;
        }

        mEventsManager.register(event, task);
    }

    @Override
    public boolean track(String trafficType, String eventType) {
        return false;
    }

    @Override
    public boolean track(String trafficType, String eventType, double value) {
        return false;
    }

    @Override
    public boolean track(String eventType) {
        return false;
    }

    @Override
    public boolean track(String eventType, double value) {
        return false;
    }

    @Override
    public boolean track(String trafficType, String eventType, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean track(String trafficType, String eventType, double value, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean track(String eventType, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean track(String eventType, double value, Map<String, Object> properties) {
        return false;
    }

    private ImpressionListener getImpressionsListener(SplitClientConfig config) {
        if (config.impressionListener() != null) {
            return config.impressionListener();
        } else {
            return new LocalhostImpressionsListener();
        }
    }

    @Override
    public boolean setAttribute(String attributeName, Object value) {
        return true;
    }

    @Nullable
    @Override
    public Object getAttribute(String attributeName) {
        return null;
    }

    @Override
    public boolean setAttributes(Map<String, Object> attributes) {
        return true;
    }

    @NonNull
    @Override
    public Map<String, Object> getAllAttributes() {
        return new HashMap<>();
    }

    @Override
    public boolean removeAttribute(String attributeName) {
        return true;
    }

    @Override
    public boolean clearAttributes() {
        return true;
    }

    private Map<String, String> buildExceptionResult(List<String> flagSets) {
        Map<String, String> result = new HashMap<>();
        Set<String> namesByFlagSets = mSplitsStorage.getNamesByFlagSets(flagSets);
        for (String featureFlagName : namesByFlagSets) {
            result.put(featureFlagName, Treatments.CONTROL);
        }

        return result;
    }

    private Map<String, SplitResult> buildExceptionResultWithConfig(List<String> flagSets) {
        Map<String, SplitResult> result = new HashMap<>();
        Set<String> namesByFlagSets = mSplitsStorage.getNamesByFlagSets(flagSets);
        for (String featureFlagName : namesByFlagSets) {
            result.put(featureFlagName, new SplitResult(Treatments.CONTROL));
        }

        return result;
    }
}
