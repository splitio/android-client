package io.split.android.client.telemetry.model;

import com.google.gson.annotations.SerializedName;

public class MethodExceptions {

    @SerializedName("t")
    private long treatment;

    @SerializedName("ts")
    private long treatments;

    @SerializedName("tc")
    private long treatmentWithConfig;

    @SerializedName("tcs")
    private long treatmentsWithConfig;

    @SerializedName("tr")
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
