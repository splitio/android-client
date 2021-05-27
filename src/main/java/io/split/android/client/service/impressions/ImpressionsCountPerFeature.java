package io.split.android.client.service.impressions;

import com.google.gson.annotations.SerializedName;

public class ImpressionsCountPerFeature {

    private static final String FIELD_FEATURE = "f";
    private static final String FIELD_TIMEFRAME = "m";
    private static final String FIELD_COUNT = "rc";

    @SerializedName(FIELD_FEATURE)
    public final String feature;

    @SerializedName(FIELD_TIMEFRAME)
    public final long timeframe;

    @SerializedName(FIELD_COUNT)
    public final int count;

    public ImpressionsCountPerFeature(String feature, long timeframe, int count) {
        this.feature = feature;
        this.timeframe = timeframe;
        this.count = count;
    }

    @Override
    public int hashCode() {
        return String.format("%s%d%d", feature, timeframe, count).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImpressionsCountPerFeature countPerFeature = (ImpressionsCountPerFeature) o;
        return feature.equals(countPerFeature.feature) &&
                timeframe == countPerFeature.timeframe &&
                count == countPerFeature.count;
    }
}
