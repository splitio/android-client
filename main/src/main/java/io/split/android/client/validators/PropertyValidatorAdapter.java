package io.split.android.client.validators;

import java.util.Map;

import io.split.android.client.tracker.TrackerLogger;
import io.split.android.client.tracker.TrackerPropertyValidator;

/**
 * Adapter that bridges the main module's PropertyValidator interface with
 * the tracker module's TrackerPropertyValidator implementation.
 */
public class PropertyValidatorAdapter implements PropertyValidator {

    private final TrackerPropertyValidator mDelegate;

    public PropertyValidatorAdapter(TrackerPropertyValidator delegate) {
        mDelegate = delegate;
    }

    @Override
    public Result validate(Map<String, Object> properties, String validationTag) {
        // Call the tracker validator with initialSizeInBytes=0 since we're not tracking
        TrackerPropertyValidator.TrackerPropertyResult trackerResult =
                mDelegate.validate(properties, 0, validationTag);

        if (trackerResult.isValid()) {
            return Result.valid(trackerResult.getProperties(), trackerResult.getSizeInBytes());
        } else {
            return Result.invalid(trackerResult.getErrorMessage(), trackerResult.getSizeInBytes());
        }
    }
}
