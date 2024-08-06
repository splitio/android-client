package io.split.android.client.storage.mysegments;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

/**
 * This DTO provides a unified way of storing my segments data in persistent storage.
 */
public class SegmentChangeDTO {

    @SerializedName("segments")
    private final List<String> mMySegments;
    @SerializedName("till")
    private final Long mTill;

    SegmentChangeDTO(List<String> mySegments, Long till) {
        mMySegments = mySegments == null ? new ArrayList<>() : mySegments;
        mTill = till;
    }

    static SegmentChangeDTO createEmpty() {
        return new SegmentChangeDTO(new ArrayList<>(), -1L);
    }

    public List<String> getMySegments() {
        return mMySegments;
    }

    public Long getTill() {
        return mTill;
    }
}
