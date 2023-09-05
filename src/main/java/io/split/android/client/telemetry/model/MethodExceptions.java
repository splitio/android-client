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

    @SerializedName("tf")
    private long treatmentsByFlagSet;

    @SerializedName("tfs")
    private long treatmentsByFlagSets;

    @SerializedName("tfc")
    private long treatmentsWithConfigByFlagSet;

    @SerializedName("tfcs")
    private long treatmentsWithConfigByFlagSets;

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

    public void setTreatmentsByFlagSet(long treatmentsByFlagSet) {
        this.treatmentsByFlagSet = treatmentsByFlagSet;
    }

    public long getTreatmentsByFlagSet() {
        return treatmentsByFlagSet;
    }

    public void setTreatmentsByFlagSets(long treatmentsByFlagSets) {
        this.treatmentsByFlagSets = treatmentsByFlagSets;
    }

    public long getTreatmentsByFlagSets() {
        return treatmentsByFlagSets;
    }

    public void setTreatmentsWithConfigByFlagSet(long treatmentsWithConfigByFlagSet) {
        this.treatmentsWithConfigByFlagSet = treatmentsWithConfigByFlagSet;
    }

    public long getTreatmentsWithConfigByFlagSet() {
        return treatmentsWithConfigByFlagSet;
    }

    public void setTreatmentsWithConfigByFlagSets(long treatmentsWithConfigByFlagSets) {
        this.treatmentsWithConfigByFlagSets = treatmentsWithConfigByFlagSets;
    }

    public long getTreatmentsWithConfigByFlagSets() {
        return treatmentsWithConfigByFlagSets;
    }

    public long getTrack() {
        return track;
    }

    public void setTrack(long track) {
        this.track = track;
    }
}
