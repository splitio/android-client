package io.split.android.telemetry.model;

public class MethodExceptions {

    private long treatment;

    private long treatments;

    private long treatmentWithConfig;

    private long treatmentsWithConfig;

    private long track;

    public long getTreatment() {
        return treatment;
    }

    public void setTreatment(long treatment) {
        this.treatment = treatment;
    }

    public long getTreatments() {
        return treatments;
    }

    public void setTreatments(long treatments) {
        this.treatments = treatments;
    }

    public long getTreatmentWithConfig() {
        return treatmentWithConfig;
    }

    public void setTreatmentWithConfig(long treatmentWithConfig) {
        this.treatmentWithConfig = treatmentWithConfig;
    }

    public long getTreatmentsWithConfig() {
        return treatmentsWithConfig;
    }

    public void setTreatmentsWithConfig(long treatmentsWithConfig) {
        this.treatmentsWithConfig = treatmentsWithConfig;
    }

    public long getTrack() {
        return track;
    }

    public void setTrack(long track) {
        this.track = track;
    }
}
