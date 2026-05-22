package io.split.android.client.tracker;

import java.util.Map;

/**
 * Validates and processes event properties.
 */
public interface TrackerPropertyValidator {

    /**
     * Validates event properties.
     *
     * @param properties       raw properties map (may be null)
     * @param initialSizeInBytes base event size in bytes (before properties), added to computed
     *                          property size to produce the total in {@link TrackerPropertyResult#getSizeInBytes()}
     * @param validationTag    tag used for log messages
     * @return validation result containing processed properties and total size
     */
    TrackerPropertyResult validate(Map<String, Object> properties, int initialSizeInBytes,
                                   String validationTag);

    class TrackerPropertyResult {
        private final boolean mIsValid;
        private final Map<String, Object> mProperties;
        private final int mSizeInBytes;
        private final String mErrorMessage;

        private TrackerPropertyResult(boolean isValid, Map<String, Object> properties,
                                      int sizeInBytes, String errorMessage) {
            mIsValid = isValid;
            mProperties = properties;
            mSizeInBytes = sizeInBytes;
            mErrorMessage = errorMessage;
        }

        public static TrackerPropertyResult valid(Map<String, Object> properties, int sizeInBytes) {
            return new TrackerPropertyResult(true, properties, sizeInBytes, null);
        }

        public static TrackerPropertyResult invalid(String errorMessage, int sizeInBytes) {
            return new TrackerPropertyResult(false, null, sizeInBytes, errorMessage);
        }

        public boolean isValid() {
            return mIsValid;
        }

        public Map<String, Object> getProperties() {
            return mProperties;
        }

        /** Total event size in bytes (initial base size + properties size). */
        public int getSizeInBytes() {
            return mSizeInBytes;
        }

        public String getErrorMessage() {
            return mErrorMessage;
        }
    }
}
