package io.split.android.client.service.impressions;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ImpressionCount {

    private static final String FIELD_PER_FEATURE_COUNTS = "pf";

    @SerializedName(FIELD_PER_FEATURE_COUNTS)
    public final List<CountPerFeature> perFeature;

    public ImpressionCount(List<CountPerFeature> countList) {
        perFeature = countList;
    }

    public static ImpressionCount fromImpressionCounterData(Map<ImpressionCounter.Key, Integer> raw) {
        List<CountPerFeature> countList = new ArrayList<>();
        for(Map.Entry<ImpressionCounter.Key, Integer> featureCount : raw.entrySet()) {
            countList.add(
                    new CountPerFeature( featureCount.getKey().featureName(),
                    featureCount.getKey().timeFrame(), featureCount.getValue())
            );
        }
        return new ImpressionCount(countList);
    }

    @Override
    public int hashCode() {
        return perFeature.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImpressionCount impressionCount = (ImpressionCount) o;

        List<CountPerFeature> other = new ArrayList<>(impressionCount.perFeature);
        List<CountPerFeature> self = new ArrayList<>(this.perFeature);
        if (other.size() != self.size()) {
            return false;
        }

        Map<String, CountPerFeature> otherFeatures = new HashMap<>();
        for (CountPerFeature otherCount : other) {
            otherFeatures.put(otherCount.feature, otherCount);
        }

        for (CountPerFeature selfCount : self) {
            CountPerFeature otherCount = otherFeatures.get(selfCount.feature);
            if (otherCount == null ||
                    selfCount.count != otherCount.count ||
                    selfCount.timeframe != otherCount.timeframe) {
                return false;
            }
        }
        return true;
    }

    public static class CountPerFeature {

        private static final String FIELD_FEATURE = "f";
        private static final String FIELD_TIMEFRAME = "m";
        private static final String FIELD_COUNT = "rc";

        @SerializedName(FIELD_FEATURE)
        public final String feature;

        @SerializedName(FIELD_TIMEFRAME)
        public final long timeframe;

        @SerializedName(FIELD_COUNT)
        public final int count;

        public CountPerFeature(String feature, long timeframe, int count) {
            this.feature = feature;
            this.timeframe = timeframe;
            this.count = count;
        }

        @Override
        public int hashCode() {
            return String.format("%s%d%d%d", feature, timeframe, count).hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CountPerFeature countPerFeature = (CountPerFeature) o;
            return feature.equals(countPerFeature.feature) &&
                    timeframe == countPerFeature.timeframe &&
                    count == countPerFeature.count;
        }
    }
}