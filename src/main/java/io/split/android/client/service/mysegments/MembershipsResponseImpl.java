package io.split.android.client.service.mysegments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class MembershipsResponseImpl implements MembershipsResponse {

    public static final int DEFAULT_TILL = -1;
    @SerializedName("ms")
    private MySegments mMySegments;

    @SerializedName("ls")
    private MySegments mMyLargeSegments;

    // TODO legacy endpoint support
    public MembershipsResponseImpl(List<String> mySegments) {
        mMySegments = new MySegments();
        Set<Membership> memberships = new HashSet<>();
        for (String segment : mySegments) {
            Membership membership = new Membership();
            membership.mName = segment;
            memberships.add(membership);
        }
        mMySegments.mMySegments = memberships;
    }

    @NonNull
    @Override
    public Set<String> getSegments() {
        return mMySegments == null ? Collections.emptySet() :
                mMySegments.getSegments();
    }

    @NonNull
    @Override
    public Set<String> getLargeSegments() {
        return mMyLargeSegments == null ? Collections.emptySet() :
                mMyLargeSegments.getSegments();
    }

    @Override
    public Long getSegmentsTill() {
        return mMySegments == null ? DEFAULT_TILL : (mMySegments.getChangeNumber() == null ? DEFAULT_TILL : mMySegments.getChangeNumber());
    }

    @Override
    public Long getLargeSegmentsTill() {
        return mMyLargeSegments == null ? DEFAULT_TILL : (mMyLargeSegments.getChangeNumber() == null ? DEFAULT_TILL : mMyLargeSegments.getChangeNumber());
    }

    private static class MySegments {
        @SerializedName("k")
        private Set<Membership> mMySegments;

        @SerializedName("cn")
        private Long mChangeNumber;

        Set<String> getSegments() {
            Set<String> names = new TreeSet<>();
            if (mMySegments != null) {
                for (Membership membership : mMySegments) {
                    names.add(membership.getName());
                }
            }

            return names;
        }

        @Nullable
        Long getChangeNumber() {
            return mChangeNumber;
        }
    }

    private static class Membership {
        @SerializedName("n")
        private String mName;

        String getName() {
            return mName;
        }
    }
}
