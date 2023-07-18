package io.split.android.client.telemetry.model;

import com.google.gson.annotations.SerializedName;

public enum ImpressionsMode {
    @SerializedName("0")
    OPTIMIZED,

    @SerializedName("1")
    DEBUG,

    @SerializedName("2")
    NONE;

    public int intValue() {
        switch (this) {
            case OPTIMIZED:
                return 0;
            case DEBUG:
                return 1;
            case NONE:
                return 2;
        }

        return 0;
    }
}
