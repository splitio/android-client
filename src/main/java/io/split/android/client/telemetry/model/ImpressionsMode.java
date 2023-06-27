package io.split.android.client.telemetry.model;

import com.google.gson.annotations.SerializedName;

public enum ImpressionsMode {
    @SerializedName("0")
    OPTIMIZED,

    @SerializedName("1")
    DEBUG;

    public int intValue() {
        switch (this) {
            case OPTIMIZED:
                return 0;
            case DEBUG:
                return 1;
        }

        return 0;
    }
}
