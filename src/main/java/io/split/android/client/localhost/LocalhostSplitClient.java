package io.split.android.client.localhost;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.Evaluator;
import io.split.android.client.EvaluatorImpl;
import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitResult;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.attributes.AttributesMerger;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.utils.Logger;
import io.split.android.client.validators.KeyValidatorImpl;
import io.split.android.client.validators.SplitValidatorImpl;
import io.split.android.client.validators.TreatmentManager;
import io.split.android.client.validators.TreatmentManagerImpl;
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
    private final String mKey;
    private final SplitEventsManager mEventsManager;
    private final Evaluator mEvaluator;
    private final TreatmentManager mTreatmentManager;
    private boolean mIsClientDestroyed = false;

    public LocalhostSplitClient(@NonNull LocalhostSplitFactory container,
                                @NonNull SplitClientConfig splitClientConfig,
                                @NonNull String key,
                                @NonNull SplitsStorage splitsStorage,
                                @NonNull SplitEventsManager eventsManager,
                                @NonNull SplitParser splitParser,
                                @NonNull AttributesManager attributesManager,
                                @NonNull AttributesMerger attributesMerger,
                                @NonNull TelemetryStorageProducer telemetryStorageProducer) {

        mFactoryRef = new WeakReference<>(checkNotNull(container));
        mKey = checkNotNull(key);
        mEventsManager = checkNotNull(eventsManager);
        mEvaluator = new EvaluatorImpl(splitsStorage, splitParser);
        mTreatmentManager = new TreatmentManagerImpl(mKey, null,
                mEvaluator, new KeyValidatorImpl(),
                new SplitValidatorImpl(), getImpressionsListener(splitClientConfig),
                splitClientConfig.labelsEnabled(), eventsManager, attributesManager, attributesMerger, telemetryStorageProducer);
    }

    @Override
    public String getTreatment(String split) {
        try {
            return mTreatmentManager.getTreatment(split, null, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e(exception);

            return Treatments.CONTROL;
        }
    }

    @Override
    public String getTreatment(String split, Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatment(split, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e(exception);

            return Treatments.CONTROL;
        }
    }

    @Override
    public SplitResult getTreatmentWithConfig(String split, Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatmentWithConfig(split, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e(exception);

            return new SplitResult(Treatments.CONTROL);
        }
    }

    @Override
    public Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatments(splits, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e(exception);

            Map<String, String> result = new HashMap<>();

            for (String split : splits) {
                result.put(split, Treatments.CONTROL);
            }

            return result;
        }
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> splits, Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatmentsWithConfig(splits, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e(exception);

            Map<String, SplitResult> result = new HashMap<>();

            for (String split : splits) {
                result.put(split, new SplitResult(Treatments.CONTROL));
            }

            return result;
        }
    }

    @Override
    public void destroy() {
        mIsClientDestroyed = true;
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
}
