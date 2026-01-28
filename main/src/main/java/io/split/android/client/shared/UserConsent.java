package io.split.android.client.shared;

public enum UserConsent {
    GRANTED,
    DECLINED,
    UNKNOWN;

    // Avoiding use of constructor
    // to make things simple for telemetry usage
    public int intValue() {
        switch (this) {
            case UNKNOWN:
                return 1;
            case GRANTED:
                return 2;
            case DECLINED:
                return 3;
        }
        // Should not reach here
        return 0;
    }
}
