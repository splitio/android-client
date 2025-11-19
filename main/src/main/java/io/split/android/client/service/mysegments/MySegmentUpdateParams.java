package io.split.android.client.service.mysegments;

public class MySegmentUpdateParams {

    private final Long mSyncDelay;
    private final Long mTargetSegmentsCn;
    private final Long mTargetLargeSegmentsCn;

    public MySegmentUpdateParams(Long syncDelay, Long targetSegmentsCn, Long targetLargeSegmentsCn) {
        mSyncDelay = syncDelay;
        mTargetSegmentsCn = targetSegmentsCn;
        mTargetLargeSegmentsCn = targetLargeSegmentsCn;
    }

    public Long getSyncDelay() {
        return mSyncDelay;
    }

    public Long getTargetSegmentsCn() {
        return mTargetSegmentsCn;
    }

    public Long getTargetLargeSegmentsCn() {
        return mTargetLargeSegmentsCn;
    }
}
