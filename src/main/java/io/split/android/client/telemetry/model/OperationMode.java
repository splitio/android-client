package io.split.android.client.telemetry.model;

public enum OperationMode {
    STANDALONE(0),
    CONSUMER(1);

    private final int numericValue;

    OperationMode(int numericValue) {
        this.numericValue = numericValue;
    }

    public int getNumericValue() {
        return numericValue;
    }
}
