package io.split.android.client.tracker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class DefaultTracker implements Tracker {

    // Estimated event size in bytes without properties
    private static final int ESTIMATED_EVENT_SIZE_WITHOUT_PROPS = 1024;

    /** Callback invoked with the validated event when tracking succeeds. */
    public interface OnEventPush {
        void accept(TrackerEvent event);
    }

    /** Callback invoked with the track latency in milliseconds. May be null to skip telemetry. */
    public interface OnTrackLatency {
        void accept(long latencyMs);
    }

    /** Callback invoked when an exception occurs during tracking. May be null to skip telemetry. */
    public interface OnTrackException {
        void accept();
    }

    @NonNull private final TrackerEventValidator mEventValidator;
    @NonNull private final TrackerLogger mTrackerLogger;
    @NonNull private final TrackerPropertyValidator mPropertyValidator;
    @NonNull private final OnEventPush mOnEventPush;
    @Nullable private final OnTrackLatency mOnTrackLatency;
    @Nullable private final OnTrackException mOnTrackException;
    private final AtomicBoolean isTrackingEnabled = new AtomicBoolean(true);

    public DefaultTracker(@NonNull TrackerEventValidator eventValidator,
                          @NonNull TrackerLogger trackerLogger,
                          @NonNull TrackerPropertyValidator propertyValidator,
                          @NonNull OnEventPush onEventPush,
                          @Nullable OnTrackLatency onTrackLatency,
                          @Nullable OnTrackException onTrackException) {
        mEventValidator = Objects.requireNonNull(eventValidator, "eventValidator must not be null");
        mTrackerLogger = Objects.requireNonNull(trackerLogger, "trackerLogger must not be null");
        mPropertyValidator = Objects.requireNonNull(propertyValidator, "propertyValidator must not be null");
        mOnEventPush = Objects.requireNonNull(onEventPush, "onEventPush must not be null");
        mOnTrackLatency = onTrackLatency;
        mOnTrackException = onTrackException;
    }

    @Override
    public void enableTracking(boolean enable) {
        isTrackingEnabled.set(enable);
    }

    @Override
    public boolean track(String key, String trafficType, String eventType,
                         Double value, Map<String, Object> properties, boolean isSdkReady) {
        if (!isTrackingEnabled.get()) {
            mTrackerLogger.v("Event not tracked because tracking is disabled");
            return false;
        }

        try {
            final String validationTag = "track";

            TrackerValidationError errorInfo = mEventValidator.validate(
                    key, trafficType, eventType, value, properties, isSdkReady);
            if (errorInfo != null) {
                if (errorInfo.isError()) {
                    mTrackerLogger.e(errorInfo.getMessage(), validationTag);
                    return false;
                }
                mTrackerLogger.log(errorInfo, validationTag);
                trafficType = trafficType.toLowerCase();
            }

            TrackerPropertyValidator.TrackerPropertyResult processedProperties =
                    mPropertyValidator.validate(properties, ESTIMATED_EVENT_SIZE_WITHOUT_PROPS, validationTag);
            if (!processedProperties.isValid()) {
                return false;
            }

            long startTime = System.currentTimeMillis();

            TrackerEvent event = new TrackerEvent();
            event.eventType = eventType;
            event.trafficType = trafficType;
            event.key = key;
            event.value = value;
            event.timestamp = System.currentTimeMillis();
            event.properties = processedProperties.getProperties();
            event.sizeInBytes = processedProperties.getSizeInBytes();
            mOnEventPush.accept(event);

            if (mOnTrackLatency != null) {
                mOnTrackLatency.accept(System.currentTimeMillis() - startTime);
            }

            return true;
        } catch (Exception exception) {
            mTrackerLogger.e("Exception while tracking event: " + exception.getMessage(), "track");
            if (mOnTrackException != null) {
                mOnTrackException.accept();
            }
        }
        return false;
    }
}
