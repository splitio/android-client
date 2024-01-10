package io.split.android.client;

public final class EvaluationResult {
    private final String mTreatment;
    private final String mLabel;
    private final Long mChangeNumber;
    private final String mConfigurations;

    public EvaluationResult(String treatment, String label) {
        this(treatment, label, null);
    }

    EvaluationResult(String treatment, String label, Long changeNumber) {
        this(treatment, label, changeNumber, null);
    }

    public EvaluationResult(String treatment, String label, Long changeNumber, String configurations) {
        mTreatment = treatment;
        mLabel = label;
        mChangeNumber = changeNumber;
        mConfigurations = configurations;
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
}
