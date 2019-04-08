package io.split.android.client;

public class SplitResult {
    private String treatment;
    private String configurations;

    public SplitResult(String treatment, String configurations) {
        this.treatment = treatment;
        this.configurations = configurations;
    }

    public SplitResult(String treatment) {
        this(treatment, null);
    }

    public String getTreatment() {
        return treatment;
    }

    public String getConfigurations() {
        return configurations;
    }
}
