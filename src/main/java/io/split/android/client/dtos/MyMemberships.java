package io.split.android.client.dtos;

import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.Set;

public class MyMemberships {
    @SerializedName("k")
    private Set<Membership> mMemberships;

    @SerializedName("cn")
    private Long mChangeNumber;

    Set<Membership> getMemberships() {
        return mMemberships == null ? Collections.emptySet() : mMemberships;
    }

    @Nullable
    Long getChangeNumber() {
        return mChangeNumber;
    }

    void setMemberships(Set<Membership> memberships) {
        mMemberships = memberships;
    }
}
