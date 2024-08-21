package io.split.android.client.dtos;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.split.android.client.service.mysegments.MembershipsResponse;

public class MembershipsResponseImpl implements MembershipsResponse {

    public static final int DEFAULT_TILL = -1;
    @SerializedName("ms")
    private MyMemberships mMySegments;

    @SerializedName("ls")
    private MyMemberships mMyLargeSegments;

    // TODO legacy endpoint support
    public MembershipsResponseImpl(List<String> mySegments) {
        mMySegments = new MyMemberships();
        Set<Membership> memberships = new HashSet<>();
        for (String segment : mySegments) {
            Membership membership = new Membership();
            membership.setName(segment);
            memberships.add(membership);
        }
        mMySegments.setMemberships(memberships);
    }

    @NonNull
    @Override
    public Set<String> getSegments() {
        return mMySegments == null ? Collections.emptySet() :
                toNames(mMySegments.getMemberships());
    }

    @NonNull
    @Override
    public Set<String> getLargeSegments() {
        return mMyLargeSegments == null ? Collections.emptySet() :
                toNames(mMyLargeSegments.getMemberships());
    }

    @Override
    public Long getSegmentsTill() {
        return mMySegments == null ? DEFAULT_TILL : (mMySegments.getChangeNumber() == null ? DEFAULT_TILL : mMySegments.getChangeNumber());
    }

    @Override
    public Long getLargeSegmentsTill() {
        return mMyLargeSegments == null ? DEFAULT_TILL : (mMyLargeSegments.getChangeNumber() == null ? DEFAULT_TILL : mMyLargeSegments.getChangeNumber());
    }

    private static Set<String> toNames(Set<Membership> memberships) {
        if (memberships == null) {
            return Collections.emptySet();
        }
        Set<String> names = new HashSet<>();
        for (Membership membership : memberships) {
            names.add(membership.getName());
        }
        return names;
    }
}
