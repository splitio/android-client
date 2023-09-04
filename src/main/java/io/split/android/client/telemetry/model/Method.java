package io.split.android.client.telemetry.model;

public enum Method {
    TREATMENT("getTreatment"),
    TREATMENTS("getTreatments"),
    TREATMENT_WITH_CONFIG("getTreatmentWithConfig"),
    TREATMENTS_WITH_CONFIG("getTreatmentsWithConfig"),
    TREATMENTS_BY_FLAG_SET("getTreatmentsByFlagSet"),
    TREATMENTS_BY_FLAG_SETS("getTreatmentsByFlagSets"),
    TREATMENTS_WITH_CONFIG_BY_FLAG_SET("getTreatmentsWithConfigByFlagSet"),
    TREATMENTS_WITH_CONFIG_BY_FLAG_SETS("getTreatmentsWithConfigByFlagSets"),
    TRACK("track");

    private final String _method;

    Method(String method) {
        _method = method;
    }

    public String getMethod() {
        return _method;
    }
}
