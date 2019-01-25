package io.split.android.client.validators;

public class ValidationConfig {
    private static final ValidationConfig mInstance = new ValidationConfig();

    private int mMaximumKeyLength = 250;
    private String mTrackEventNamePattern = "^[a-zA-Z0-9][-_.:a-zA-Z0-9]{0,79}$";

    public static ValidationConfig getInstance() {
        return mInstance;
    }

    private ValidationConfig() {
    }

    public int getMaximumKeyLength() {
        return mMaximumKeyLength;
    }

    public void setMaximumKeyLength(int maximumKeyLength) {
        this.mMaximumKeyLength = maximumKeyLength;
    }

    public String getTrackEventNamePattern() {
        return mTrackEventNamePattern;
    }

    public void setTrackEventNamePattern(String trackEventNamePattern) {
        this.mTrackEventNamePattern = trackEventNamePattern;
    }
}
