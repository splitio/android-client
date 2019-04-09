package io.split.android.client;

public class SplitResult {
    private String treatment;
    private String config;

    public SplitResult(String treatment, String config) {
        this.treatment = treatment;
        this.config = config;
    }

    public SplitResult(String treatment) {
        this(treatment, null);
    }

    public String treatment() {
        return treatment;
    }

    public String config() {
        return config;
    }
}
