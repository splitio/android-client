package io.split.android.client;

import androidx.annotation.VisibleForTesting;

public final class EvaluationResult {
    private final String mTreatment;
    private final String mLabel;
    private final Long mChangeNumber;
    private final String mConfigurations;
    private final boolean mTrackImpression;

    @VisibleForTesting
    public EvaluationResult(String treatment, String label) {
        this(treatment, label, null, null, true);
    }

    public EvaluationResult(String treatment, String label, boolean trackImpression) {
        this(treatment, label, null, null, trackImpression);
    }

    EvaluationResult(String treatment, String label, Long changeNumber, boolean trackImpression) {
        this(treatment, label, changeNumber, null, trackImpression);
    }

    public EvaluationResult(String treatment, String label, Long changeNumber, String configurations, boolean trackImpression) {
        mTreatment = treatment;
        mLabel = label;
        mChangeNumber = changeNumber;
        mConfigurations = configurations;
        mTrackImpression = trackImpression;
    }

    public String getTreatment() {
        return mTreatment;
    }

    public String getLabel() {
        return mLabel;
    }

    public Long getChangeNumber() {
        return mChangeNumber;
    }

    public String getConfigurations() {
        return mConfigurations;
    }

    public boolean getTrackImpression() {
        return mTrackImpression;
    }
}
