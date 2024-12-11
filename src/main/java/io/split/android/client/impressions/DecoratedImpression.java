package io.split.android.client.impressions;

import java.util.Map;

public class DecoratedImpression extends Impression {
    
    private final boolean mTrackImpressions;
    
    public DecoratedImpression(String key, String bucketingKey, String split, String treatment, long time, String appliedRule, Long changeNumber, Map<String, Object> atributes, boolean trackImpressions) {
        super(key, bucketingKey, split, treatment, time, appliedRule, changeNumber, atributes);
        mTrackImpressions = trackImpressions;
    }

    public boolean getTrackImpressions() {
        return mTrackImpressions;
    }
}
