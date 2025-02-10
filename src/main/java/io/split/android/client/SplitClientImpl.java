package io.split.android.client;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.validators.SplitValidator;
import io.split.android.client.validators.TreatmentManager;
import io.split.android.client.validators.ValidationMessageLogger;
import io.split.android.client.validators.ValidationMessageLoggerImpl;
import io.split.android.engine.experiments.SplitParser;

public final class SplitClientImpl implements SplitClient {

    private final WeakReference<SplitFactory> mSplitFactory;
    private final WeakReference<SplitClientContainer> mClientContainer;
    private final SplitClientConfig mConfig;
    private final Key mKey;
    private final SplitEventsManager mEventsManager;
    private final TreatmentManager mTreatmentManager;
    private final ValidationMessageLogger mValidationLogger;
    private final AttributesManager mAttributesManager;
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
                           SplitValidator splitValidator,
                           TreatmentManager treatmentManager) {
        checkNotNull(splitParser);
        checkNotNull(impressionListener);

        mSplitFactory = new WeakReference<>(checkNotNull(container));
        mClientContainer = new WeakReference<>(checkNotNull(clientContainer));
        mKey = checkNotNull(key);
        mConfig = checkNotNull(config);
        mEventsManager = checkNotNull(eventsManager);
        mEventsTracker = checkNotNull(eventsTracker);
        mValidationLogger = new ValidationMessageLoggerImpl();
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
                    if (splitFactory instanceof SplitFactoryImpl) {
                        try {
                            ((SplitFactoryImpl) splitFactory).checkClients();
                        } catch (ClassCastException ignored) {

                        }
                    }
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
    public String getTreatment(String featureFlagName) {
        return getTreatment(featureFlagName, Collections.emptyMap());
    }

    @Override
    public String getTreatment(String featureFlagName, Map<String, Object> attributes) {
        return mTreatmentManager.getTreatment(featureFlagName, attributes, mIsClientDestroyed);
    }

    @Override
    public SplitResult getTreatmentWithConfig(String featureFlagName, Map<String, Object> attributes) {
        return mTreatmentManager.getTreatmentWithConfig(featureFlagName, attributes, mIsClientDestroyed);
    }

    @Override
    public Map<String, String> getTreatments(List<String> featureFlagNames, Map<String, Object> attributes) {
        return mTreatmentManager.getTreatments(featureFlagNames, attributes, mIsClientDestroyed);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> featureFlagNames, Map<String, Object> attributes) {
        return mTreatmentManager.getTreatmentsWithConfig(featureFlagNames, attributes, mIsClientDestroyed);
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes) {
        return mTreatmentManager.getTreatmentsByFlagSet(flagSet, attributes, mIsClientDestroyed);
    }

    @Override
    public Map<String, String> getTreatmentsByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes) {
        return mTreatmentManager.getTreatmentsByFlagSets(flagSets, attributes, mIsClientDestroyed);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes) {
        return mTreatmentManager.getTreatmentsWithConfigByFlagSet(flagSet, attributes, mIsClientDestroyed);
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes) {
        return mTreatmentManager.getTreatmentsWithConfigByFlagSets(flagSets, attributes, mIsClientDestroyed);
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
