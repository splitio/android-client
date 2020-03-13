package io.split.android.client;

import java.util.Map;

public class ProcessedEventProperties {
    private final boolean isValid;
    private final Map<String, Object> properties;
    private final int sizeInBytes;

    static public ProcessedEventProperties InvalidProperties() {
        return new ProcessedEventProperties(false, null, 0);
    }

    public ProcessedEventProperties(boolean isValid, Map<String, Object> properties, int sizeInBytes) {
        this.isValid = isValid;
        this.properties = properties;
        this.sizeInBytes = sizeInBytes;
    }

    public boolean isValid() {
        return isValid;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public int getSizeInBytes() {
        return sizeInBytes;
    }
}
