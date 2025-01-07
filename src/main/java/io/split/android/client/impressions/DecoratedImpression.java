package io.split.android.client.impressions;

public class DecoratedImpression {

    private final Impression mImpression;
    private final boolean mDisabled;
    
    public DecoratedImpression(Impression impression, boolean disabled) {
        mImpression = impression;
        mDisabled = disabled;
    }

    public Impression getImpression() {
        return mImpression;
    }

    public boolean isImpressionsDisabled() {
        return mDisabled;
    }
}
