package io.split.android.client;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.split.android.client.api.Key;
import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.shared.SplitClientContainer;
import io.split.android.client.telemetry.model.Method;
import io.split.android.client.telemetry.storage.TelemetryStorageProducer;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.TreatmentManager;
import io.split.android.client.validators.TreatmentManagerHelper;
import io.split.android.client.validators.ValidationMessageLogger;
import io.split.android.client.validators.ValidationMessageLoggerImpl;
import io.split.android.engine.experiments.SplitParser;
import io.split.android.grammar.Treatments;

public final class SplitClientImpl implements SplitClient {

    private final WeakReference<SplitFactory> mSplitFactory;
    private final WeakReference<SplitClientContainer> mClientContainer;
    private final SplitClientConfig mConfig;
    private final Key mKey;
    private final SplitEventsManager mEventsManager;
    private final TreatmentManager mTreatmentManager;
    private final ValidationMessageLogger mValidationLogger;
    private final AttributesManager mAttributesManager;
    private final TelemetryStorageProducer mTelemetryStorageProducer;
    private final SplitValidator mSplitValidator;
    private final EventsTracker mEventsTracker;

    private static final double TRACK_DEFAULT_VALUE = 0.0;

    private boolean mIsClientDestroyed = false;

    public SplitClientImpl(SplitFactory container,
                           SplitClientContainer clientContainer,
                           Key key,
                           SplitParser splitParser,
                           ImpressionListener impressionListener,
                           SplitClientConfig config,
                           SplitEventsManager eventsManager,
                           EventsTracker eventsTracker,
                           AttributesManager attributesManager,
                           TelemetryStorageProducer telemetryStorageProducer,
                           SplitValidator splitValidator,
                           TreatmentManager treatmentManager) {
        this(container,
                clientContainer,
                key,
                splitParser,
                impressionListener,
                config,
                eventsManager,
                eventsTracker,
                attributesManager,
                telemetryStorageProducer,
                treatmentManager,
                splitValidator);
    }

    @VisibleForTesting
    public SplitClientImpl(SplitFactory container,
                           SplitClientContainer clientContainer,
                           Key key,
                           SplitParser splitParser,
                           ImpressionListener impressionListener,
                           SplitClientConfig config,
                           SplitEventsManager eventsManager,
                           EventsTracker eventsTracker,
                           AttributesManager attributesManager,
                           TelemetryStorageProducer telemetryStorageProducer,
                           TreatmentManager treatmentManager,
                           SplitValidator splitValidator) {
        checkNotNull(splitParser);
        checkNotNull(impressionListener);

        mSplitFactory = new WeakReference<>(checkNotNull(container));
        mClientContainer = new WeakReference<>(checkNotNull(clientContainer));
        mKey = checkNotNull(key);
        mConfig = checkNotNull(config);
        mEventsManager = checkNotNull(eventsManager);
        mEventsTracker = checkNotNull(eventsTracker);
        mValidationLogger = new ValidationMessageLoggerImpl();
        mTelemetryStorageProducer = telemetryStorageProducer;
        mTreatmentManager = treatmentManager;
        mAttributesManager = checkNotNull(attributesManager);
        mSplitValidator = checkNotNull(splitValidator);
    }

    @Override
    public void destroy() {
        mIsClientDestroyed = true;
        SplitClientContainer splitClientContainer = mClientContainer.get();
        if (splitClientContainer != null) {
            splitClientContainer.remove(mKey);

            if (splitClientContainer.getAll().isEmpty()) {
                SplitFactory splitFactory = mSplitFactory.get();
                if (splitFactory != null) {
                    splitFactory.destroy();
                }
            }
        }
    }

    @Override
    public void flush() {
        SplitFactory splitFactory = mSplitFactory.get();
        if (splitFactory != null) {
            splitFactory.flush();
        }
    }

    @Override
    public boolean isReady() {
        return mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY);
    }

    @Override
    public String getTreatment(String split) {
        return getTreatment(split, Collections.emptyMap());
    }

    @Override
    public String getTreatment(String split, Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatment(split, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e("Client getTreatment exception", exception);

            mTelemetryStorageProducer.recordException(Method.TREATMENT);

            return Treatments.CONTROL;
        }
    }

    @Override
    public SplitResult getTreatmentWithConfig(String split, Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatmentWithConfig(split, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e("Client getTreatmentWithConfig exception", exception);

            mTelemetryStorageProducer.recordException(Method.TREATMENT_WITH_CONFIG);

            return new SplitResult(Treatments.CONTROL, TreatmentLabels.EXCEPTION);
        }
    }

    @Override
    public Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatments(splits, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e("Client getTreatments exception", exception);

            mTelemetryStorageProducer.recordException(Method.TREATMENTS);

            return TreatmentManagerHelper.controlTreatmentsForSplits(splits, mSplitValidator);
        }
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> splits, Map<String, Object> attributes) {
        try {
            return mTreatmentManager.getTreatmentsWithConfig(splits, attributes, mIsClientDestroyed);
        } catch (Exception exception) {
            Logger.e("Client getTreatmentsWithConfig exception", exception);

            mTelemetryStorageProducer.recordException(Method.TREATMENTS_WITH_CONFIG);

            return TreatmentManagerHelper.controlTreatmentsForSplitsWithConfig(splits, mSplitValidator);
        }
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
        return track(mKey.matchingKey(), trafficType, eventType, TRACK_DEFAULT_VALUE, null);
    }

    @Override
    public boolean track(String trafficType, String eventType, double value) {
        return track(mKey.matchingKey(), trafficType, eventType, value, null);
    }

    @Override
    public boolean track(String eventType) {
        return track(mKey.matchingKey(), mConfig.trafficType(), eventType, TRACK_DEFAULT_VALUE, null);
    }

    @Override
    public boolean track(String eventType, double value) {
        return track(mKey.matchingKey(), mConfig.trafficType(), eventType, value, null);
    }

    @Override
    public boolean track(String trafficType, String eventType, Map<String, Object> properties) {
        return track(mKey.matchingKey(), trafficType, eventType, TRACK_DEFAULT_VALUE, properties);
    }

    @Override
    public boolean track(String trafficType, String eventType, double value, Map<String, Object> properties) {
        return track(mKey.matchingKey(), trafficType, eventType, value, properties);
    }

    @Override
    public boolean track(String eventType, Map<String, Object> properties) {
        return track(mKey.matchingKey(), mConfig.trafficType(), eventType, TRACK_DEFAULT_VALUE, properties);
    }

    @Override
    public boolean track(String eventType, double value, Map<String, Object> properties) {
        return track(mKey.matchingKey(), mConfig.trafficType(), eventType, value, properties);
    }

    private boolean track(String key, String trafficType, String eventType, double value, Map<String, Object> properties) {
        if (mIsClientDestroyed) {
            mValidationLogger.e("Client has already been destroyed - no calls possible", "track");
            return false;
        }
        boolean isSdkReady = mEventsManager.eventAlreadyTriggered(SplitEvent.SDK_READY);
        return mEventsTracker.track(key, trafficType, eventType, value, properties, isSdkReady);
    }

    @Override
    public boolean setAttribute(String attributeName, Object value) {
        try {
            return mAttributesManager.setAttribute(attributeName, value);
        } catch (Exception exception) {
            Logger.e("Error setting attribute: " + exception.getLocalizedMessage());

            return false;
        }
    }

    @Nullable
    @Override
    public Object getAttribute(String attributeName) {
        try {
            return mAttributesManager.getAttribute(attributeName);
        } catch (Exception exception) {
            Logger.e("Error getting attribute: " + exception.getLocalizedMessage());

            return null;
        }
    }

    @Override
    public boolean setAttributes(Map<String, Object> attributes) {
        try {
            return mAttributesManager.setAttributes(attributes);
        } catch (Exception exception) {
            Logger.e("Error setting attributes: " + exception.getLocalizedMessage());

            return false;
        }
    }

    @NonNull
    @Override
    public Map<String, Object> getAllAttributes() {
        try {
            return mAttributesManager.getAllAttributes();
        } catch (Exception exception) {
            Logger.e("Error getting attributes: " + exception.getLocalizedMessage());

            return Collections.emptyMap();
        }
    }

    @Override
    public boolean removeAttribute(String attributeName) {
        try {
            return mAttributesManager.removeAttribute(attributeName);
        } catch (Exception exception) {
            Logger.e("Error removing attribute: " + exception.getLocalizedMessage());

            return false;
        }
    }

    @Override
    public boolean clearAttributes() {
        try {
            return mAttributesManager.clearAttributes();
        } catch (Exception exception) {
            Logger.e("Error clearing attributes: " + exception.getLocalizedMessage());

            return false;
        }
    }
}
