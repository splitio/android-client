package io.split.android.client.service.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.dtos.SegmentResponse;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.storage.mysegments.MySegmentsStorage;

public class MySegmentsTaskFactoryConfiguration {

    private final HttpFetcher<? extends SegmentResponse> mHttpFetcher;
    private final MySegmentsStorage mStorage;
    private final SplitEventsManager mEventsManager;
    private final MySegmentsSyncTaskConfig mMySegmentsSyncTaskConfig;
    private final MySegmentsUpdateTaskConfig mMySegmentsUpdateTaskConfig;
    private final MySegmentsOverwriteTaskConfig mMySegmentsOverwriteTaskConfig;
    private final LoadMySegmentsTaskConfig mLoadMySegmentsTaskConfig;

    private MySegmentsTaskFactoryConfiguration(@NonNull HttpFetcher<? extends SegmentResponse> httpFetcher,
                                               @NonNull MySegmentsStorage storage,
                                               @NonNull SplitEventsManager eventsManager,
                                               @NonNull MySegmentsSyncTaskConfig mySegmentsSyncTaskConfig,
                                               @NonNull MySegmentsUpdateTaskConfig mySegmentsUpdateTaskConfig,
                                               @NonNull MySegmentsOverwriteTaskConfig mySegmentsOverwriteTaskConfig,
                                               @NonNull LoadMySegmentsTaskConfig loadMySegmentsTaskConfig) {
        mHttpFetcher = checkNotNull(httpFetcher);
        mStorage = checkNotNull(storage);
        mEventsManager = checkNotNull(eventsManager);
        mMySegmentsSyncTaskConfig = checkNotNull(mySegmentsSyncTaskConfig);
        mMySegmentsUpdateTaskConfig = checkNotNull(mySegmentsUpdateTaskConfig);
        mMySegmentsOverwriteTaskConfig = checkNotNull(mySegmentsOverwriteTaskConfig);
        mLoadMySegmentsTaskConfig = checkNotNull(loadMySegmentsTaskConfig);
    }

    @NonNull
    public HttpFetcher<? extends SegmentResponse> getHttpFetcher() {
        return mHttpFetcher;
    }

    @NonNull
    public MySegmentsStorage getStorage() {
        return mStorage;
    }

    @NonNull
    public SplitEventsManager getEventsManager() {
        return mEventsManager;
    }

    @NonNull
    public MySegmentsSyncTaskConfig getMySegmentsSyncTaskConfig() {
        return mMySegmentsSyncTaskConfig;
    }

    @NonNull
    public MySegmentsUpdateTaskConfig getMySegmentsUpdateTaskConfig() {
        return mMySegmentsUpdateTaskConfig;
    }

    @NonNull
    public MySegmentsOverwriteTaskConfig getMySegmentsOverwriteTaskConfig() {
        return mMySegmentsOverwriteTaskConfig;
    }

    @NonNull
    public LoadMySegmentsTaskConfig getLoadMySegmentsTaskConfig() {
        return mLoadMySegmentsTaskConfig;
    }

    public static MySegmentsTaskFactoryConfiguration getForMySegments(@NonNull HttpFetcher<? extends SegmentResponse> httpFetcher,
                                                                      @NonNull MySegmentsStorage storage,
                                                                      @NonNull SplitEventsManager eventsManager) {
        return new MySegmentsTaskFactoryConfiguration(httpFetcher,
                storage,
                eventsManager,
                MySegmentsSyncTaskConfig.getForMySegments(),
                MySegmentsUpdateTaskConfig.getForMySegments(),
                MySegmentsOverwriteTaskConfig.getForMySegments(),
                LoadMySegmentsTaskConfig.getForMySegments());
    }

    public static MySegmentsTaskFactoryConfiguration getForMyLargeSegments(@NonNull HttpFetcher<? extends SegmentResponse> httpFetcher,
                                                                           @NonNull MySegmentsStorage storage,
                                                                           @NonNull SplitEventsManager eventsManager) {
        return new MySegmentsTaskFactoryConfiguration(httpFetcher,
                storage,
                eventsManager,
                MySegmentsSyncTaskConfig.getForMyLargeSegments(),
                MySegmentsUpdateTaskConfig.getForMyLargeSegments(),
                MySegmentsOverwriteTaskConfig.getForMyLargeSegments(),
                LoadMySegmentsTaskConfig.getForMyLargeSegments());
    }
}
