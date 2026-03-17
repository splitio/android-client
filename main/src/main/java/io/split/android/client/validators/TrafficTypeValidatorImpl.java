package io.split.android.client.validators;

import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.tracker.TrafficTypeValidator;

/**
 * Implementation of {@link TrafficTypeValidator} that delegates to {@link SplitsStorage}.
 * <p>
 * This implementation validates traffic type names by checking if they exist in the
 * Split storage. It bridges the tracker module's abstraction with the SDK's storage layer.
 */
public class TrafficTypeValidatorImpl implements TrafficTypeValidator {
    private final SplitsStorage mSplitsStorage;

    public TrafficTypeValidatorImpl(SplitsStorage splitsStorage) {
        mSplitsStorage = splitsStorage;
    }

    @Override
    public boolean isValid(String trafficTypeName) {
        return mSplitsStorage.isValidTrafficType(trafficTypeName);
    }
}
