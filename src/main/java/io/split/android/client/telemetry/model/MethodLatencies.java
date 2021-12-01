package io.split.android.client.telemetry.model;

import java.util.List;

public class MethodLatencies {

    private List<Long> treatment;

    private List<Long> treatments;

    private List<Long> treatmentWithConfig;

    private List<Long> treatmentsWithConfig;

    private List<Long> track;

    public List<Long> getTreatment() {
        return treatment;
    }

    public void setTreatment(List<Long> treatment) {
        this.treatment = treatment;
    }

    public List<Long> getTreatments() {
        return treatments;
    }

    public void setTreatments(List<Long> treatments) {
        this.treatments = treatments;
    }

    public List<Long> getTreatmentWithConfig() {
        return treatmentWithConfig;
    }

    public void setTreatmentWithConfig(List<Long> treatmentWithConfig) {
        this.treatmentWithConfig = treatmentWithConfig;
    }

    public List<Long> getTreatmentsWithConfig() {
        return treatmentsWithConfig;
    }

    public void setTreatmentsWithConfig(List<Long> treatmentsWithConfig) {
        this.treatmentsWithConfig = treatmentsWithConfig;
    }

    public List<Long> getTrack() {
        return track;
    }

    public void setTrack(List<Long> track) {
        this.track = track;
    }
}
