package io.split.android.client.telemetry.model;

import com.google.gson.annotations.SerializedName;

public enum EventTypeEnum {

    @SerializedName("0")
    CONNECTION_ESTABLISHED(0),
    @SerializedName("10")
    OCCUPANCY_PRI(10),
    @SerializedName("20")
    OCCUPANCY_SEC(20),
    @SerializedName("30")
    STREAMING_STATUS(30),
    @SerializedName("40")
    SSE_CONNECTION_ERROR(40),
    @SerializedName("50")
    TOKEN_REFRESH(50),
    @SerializedName("60")
    ABLY_ERROR(60),
    @SerializedName("70")
    SYNC_MODE_UPDATE(70);

    private final int numericValue;

    EventTypeEnum(int numericValue) {
        this.numericValue = numericValue;
    }

    public int getNumericValue() {
        return numericValue;
    }
}
