package io.split.android.client.dtos;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.HashSet;
import java.util.Set;

public class Prerequisite {

    @SerializedName("n")
    private String name;

    @SerializedName("ts")
    private Set<String> treatments;

    public Prerequisite() {
    }

    public Prerequisite(String name, Set<String> treatments) {
        this.name = name;
        this.treatments = treatments;
    }

    @NonNull
    public String getFlagName() {
        return name == null ? "" : name;
    }

    @NonNull
    public Set<String> getTreatments() {
        return treatments == null ? new HashSet<>() : treatments;
    }
}
