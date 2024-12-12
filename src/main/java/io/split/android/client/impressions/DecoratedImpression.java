package io.split.android.client.impressions;

public class DecoratedImpression {

    private final Impression mImpression;
    private final boolean mTrackImpressions;
    
    public DecoratedImpression(Impression impression, boolean trackImpressions) {
        mImpression = impression;
        mTrackImpressions = trackImpressions;
    }

    public Impression getImpression() {
        return mImpression;
    }

    public boolean getTrackImpressions() {
        return mTrackImpressions;
    }
}
