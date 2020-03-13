package io.split.android.client;

public final class EvaluationResult {
    private final String _treatment;
    private final String _label;
    private final Long _changeNumber;
    private final String _configurations;

    public EvaluationResult(String treatment, String label) {
        this(treatment, label, null);
    }

    private EvaluationResult(String treatment, String label, Long changeNumber) {
        this(treatment, label, changeNumber, null);
    }

    public EvaluationResult(String treatment, String label, Long changeNumber, String configurations) {
        _treatment = treatment;
        _label = label;
        _changeNumber = changeNumber;
        _configurations = configurations;
    }

    public String getTreatment() {
        return _treatment;
    }

    public String getLabel() {
        return _label;
    }

    public Long getChangeNumber() {
        return _changeNumber;
    }

    public String getConfigurations() {
        return _configurations;
    }
}
