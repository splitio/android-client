package io.split.android.client;

import androidx.annotation.VisibleForTesting;

public final class EvaluationResult {
    private final String mTreatment;
    private final String mLabel;
    private final Long mChangeNumber;
    private final String mConfigurations;
    private final boolean mImpressionsDisabled;

    @VisibleForTesting
    public EvaluationResult(String treatment, String label) {
        this(treatment, label, null, null, false);
    }

    public EvaluationResult(String treatment, String label, boolean impressionsDisabled) {
        this(treatment, label, null, null, impressionsDisabled);
    }

    EvaluationResult(String treatment, String label, Long changeNumber, boolean impressionsDisabled) {
        this(treatment, label, changeNumber, null, impressionsDisabled);
    }

    public EvaluationResult(String treatment, String label, Long changeNumber, String configurations, boolean impressionsDisabled) {
        mTreatment = treatment;
        mLabel = label;
        mChangeNumber = changeNumber;
        mConfigurations = configurations;
        mImpressionsDisabled = impressionsDisabled;
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

    public boolean isImpressionsDisabled() {
        return mImpressionsDisabled;
    }
}
