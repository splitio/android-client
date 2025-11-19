package io.split.android.client.storage.cipher;

import androidx.annotation.NonNull;

public enum SplitEncryptionLevel {

    /**
     * AES 128 CBC.
     */
    AES_128_CBC("AES_128_CBC"),

    /**
     * No encryption; plain text.
     */
    NONE("NONE");

    private final String mDescription;

    SplitEncryptionLevel(String description) {
        mDescription = description;
    }

    @NonNull
    @Override
    public String toString() {
        return mDescription;
    }

    public static SplitEncryptionLevel fromString(String stringValue) {
        for (SplitEncryptionLevel value : SplitEncryptionLevel.values()) {
            if (value.mDescription.equalsIgnoreCase(stringValue)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Invalid string value for SplitEncryptionLevel: " + stringValue);
    }
}
