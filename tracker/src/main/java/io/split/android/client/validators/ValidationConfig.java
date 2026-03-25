package io.split.android.client.validators;

/**
 * Default config for validations component
 * This configuration should be overwritten in factory or client instantiation
 * with Split Config values
 */
public class ValidationConfig {
    private static final ValidationConfig mInstance = new ValidationConfig();

    private int mMaximumKeyLength = 250;
    private String mTrackEventNamePattern = "^[a-zA-Z0-9][-_.:a-zA-Z0-9]{0,79}$";
    private  static final int maximumEventPropertyBytes = 32768;

    public static ValidationConfig getInstance() {
        return mInstance;
    }

    private ValidationConfig() {
    }

    /**
     * Maximum character length for Matching key
     * and Bucketing key
     *
     * @return int
     */
    public int getMaximumKeyLength() {
        return mMaximumKeyLength;
    }

    /**
     * Sets the maximum character length for Matching key
     * and Bucketing key
     *
     *  @param maximumKeyLength: Maximum key length allowed
     */
    public void setMaximumKeyLength(int maximumKeyLength) {
        this.mMaximumKeyLength = maximumKeyLength;
    }

    /**
     * Regex used to validate Track Event Name
     *
     * @return String
     */
    public String getTrackEventNamePattern() {
        return mTrackEventNamePattern;
    }

    /**
     * Sets the regex used to validate Track Event Name
     *
     *  @param trackEventNamePattern: Regex pattern String
     */
    public void setTrackEventNamePattern(String trackEventNamePattern) {
        this.mTrackEventNamePattern = trackEventNamePattern;
    }

    /**
     * Maximum bytes size for a property value or name
     *
     *  @return Max size in bytes
     */
    public int getMaximumEventPropertyBytes() {
        return maximumEventPropertyBytes;
    }
}
