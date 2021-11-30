package io.split.android.telemetry.model;

public enum Method {
    TREATMENT("getTreatment"),
    TREATMENTS("getTreatments"),
    TREATMENT_WITH_CONFIG("getTreatmentWithConfig"),
    TREATMENTS_WITH_CONFIG("getTreatmentsWithConfig"),
    TRACK("track");

    private final String _method;

    Method(String method) {
        _method = method;
    }

    public String getMethod() {
        return _method;
    }
}
