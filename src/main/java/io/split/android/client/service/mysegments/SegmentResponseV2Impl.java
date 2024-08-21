package io.split.android.client.service.mysegments;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SegmentResponseV2Impl implements SegmentResponseV2 {

    public static final int DEFAULT_TILL = -1;
    @SerializedName("mySegments")
    private MySegments mMySegments;

    @SerializedName("myLargeSegments")
    private MyLargeSegments mMyLargeSegments;

    // TODO legacy endpoint support
    public SegmentResponseV2Impl(List<String> mySegments) {
        mMySegments = new MySegments();
        mMySegments.mMySegments = new HashSet<>(mySegments);
    }

    @NonNull
    @Override
    public Set<String> getSegments() {
        return mMySegments == null || mMySegments.getMySegments() == null ? Collections.emptySet() :
                mMySegments.getMySegments();
    }

    @NonNull
    @Override
    public Set<String> getLargeSegments() {
        return mMyLargeSegments == null || mMyLargeSegments.getMyLargeSegments() == null ? Collections.emptySet() :
                mMyLargeSegments.getMyLargeSegments();
    }

    @Override
    public Long getSegmentsTill() {
        return mMySegments == null ? DEFAULT_TILL : (mMySegments.getTill() == null ? DEFAULT_TILL : mMySegments.getTill());
    }

    @Override
    public Long getLargeSegmentsTill() {
        return mMyLargeSegments == null ? DEFAULT_TILL : (mMyLargeSegments.getTill() == null ? DEFAULT_TILL : mMyLargeSegments.getTill());
    }

    private static class MySegments {
        @SerializedName("mySegments")
        private Set<String> mMySegments;

        @SerializedName("till")
        private Long mTill;

        Set<String> getMySegments() {
            return mMySegments;
        }

        @Nullable
        Long getTill() {
            return mTill;
        }
    }

    private static class MyLargeSegments {
        @SerializedName("myLargeSegments")
        private Set<String> mMyLargeSegments;

        @SerializedName("till")
        private Long mTill;

        Set<String> getMyLargeSegments() {
            return mMyLargeSegments;
        }

        @Nullable
        Long getTill() {
            return mTill;
        }
    }
}
