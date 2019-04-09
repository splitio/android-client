package io.split.android.client;

public class SplitResult {
    private String treatment;
    private String config;

    public SplitResult(String treatment, String configs) {
        this.treatment = treatment;
        this.config = configs;
    }

    public SplitResult(String treatment) {
        this(treatment, null);
    }

    public String treatment() {
        return treatment;
    }

    public String configs() {
        return config;
    }
}
