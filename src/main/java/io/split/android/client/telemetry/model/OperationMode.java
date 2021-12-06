package io.split.android.client.telemetry.model;

import com.google.gson.annotations.SerializedName;

public enum OperationMode {
    @SerializedName("0")
    STANDALONE(0),
    @SerializedName("1")
    CONSUMER(1);

    private final int numericValue;

    OperationMode(int numericValue) {
        this.numericValue = numericValue;
    }

    public int getNumericValue() {
        return numericValue;
    }
}
