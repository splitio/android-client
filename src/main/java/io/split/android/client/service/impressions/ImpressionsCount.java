package io.split.android.client.service.impressions;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImpressionsCount {

    private static final String FIELD_PER_FEATURE_COUNTS = "pf";

    @SerializedName(FIELD_PER_FEATURE_COUNTS)
    public final List<ImpressionsCountPerFeature> perFeature;

    public ImpressionsCount(List<ImpressionsCountPerFeature> countList) {
        perFeature = countList;
    }

    public static ImpressionsCount fromImpressionCounterData(Map<ImpressionsCounter.Key, Integer> raw) {
        List<ImpressionsCountPerFeature> countList = new ArrayList<>();
        for(Map.Entry<ImpressionsCounter.Key, Integer> featureCount : raw.entrySet()) {
            countList.add(
                    new ImpressionsCountPerFeature( featureCount.getKey().featureName(),
                    featureCount.getKey().timeFrame(), featureCount.getValue())
            );
        }
        return new ImpressionsCount(countList);
    }

    @Override
    public int hashCode() {
        return perFeature.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImpressionsCount impressionsCount = (ImpressionsCount) o;

        List<ImpressionsCountPerFeature> other = new ArrayList<>(impressionsCount.perFeature);
        List<ImpressionsCountPerFeature> self = new ArrayList<>(this.perFeature);
        if (other.size() != self.size()) {
            return false;
        }

        Map<String, ImpressionsCountPerFeature> otherFeatures = new HashMap<>();
        for (ImpressionsCountPerFeature otherCount : other) {
            otherFeatures.put(otherCount.feature, otherCount);
        }

        for (ImpressionsCountPerFeature selfCount : self) {
            ImpressionsCountPerFeature otherCount = otherFeatures.get(selfCount.feature);
            if (otherCount == null ||
                    selfCount.count != otherCount.count ||
                    selfCount.timeframe != otherCount.timeframe) {
                return false;
            }
        }
        return true;
    }
}