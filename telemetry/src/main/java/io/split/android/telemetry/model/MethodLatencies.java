package io.split.android.telemetry.model;

import java.util.List;

public class MethodLatencies {

    private List<Integer> treatment;

    private List<Integer> treatments;

    private List<Integer> treatmentWithConfig;

    private List<Integer> treatmentsWithConfig;

    private List<Integer> track;

    public List<Integer> getTreatment() {
        return treatment;
    }

    public void setTreatment(List<Integer> treatment) {
        this.treatment = treatment;
    }

    public List<Integer> getTreatments() {
        return treatments;
    }

    public void setTreatments(List<Integer> treatments) {
        this.treatments = treatments;
    }

    public List<Integer> getTreatmentWithConfig() {
        return treatmentWithConfig;
    }

    public void setTreatmentWithConfig(List<Integer> treatmentWithConfig) {
        this.treatmentWithConfig = treatmentWithConfig;
    }

    public List<Integer> getTreatmentsWithConfig() {
        return treatmentsWithConfig;
    }

    public void setTreatmentsWithConfig(List<Integer> treatmentsWithConfig) {
        this.treatmentsWithConfig = treatmentsWithConfig;
    }

    public List<Integer> getTrack() {
        return track;
    }

    public void setTrack(List<Integer> track) {
        this.track = track;
    }
}
