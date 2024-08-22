package io.split.android.client.service.mysegments;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import io.split.android.client.dtos.AllSegmentsChange;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.storage.mysegments.MySegmentsStorage;

public class MySegmentsTaskFactoryConfiguration {

    private final HttpFetcher<AllSegmentsChange> mHttpFetcher;
    private final MySegmentsStorage mMySegmentsStorage;
    private final SplitEventsManager mEventsManager;
    private final MySegmentsSyncTaskConfig mMySegmentsSyncTaskConfig;
    private final MySegmentsUpdateTaskConfig mMySegmentsUpdateTaskConfig;
    private final MySegmentsOverwriteTaskConfig mMySegmentsOverwriteTaskConfig;
    private final MySegmentsUpdateTaskConfig mMyLargeSegmentsUpdateTaskConfig;
    private final LoadMySegmentsTaskConfig mLoadMySegmentsTaskConfig;
    private final MySegmentsStorage mMyLargeSegmentsStorage;

    private MySegmentsTaskFactoryConfiguration(@NonNull HttpFetcher<AllSegmentsChange> httpFetcher,
                                               @NonNull MySegmentsStorage storage,
                                               @NonNull MySegmentsStorage myLargeSegmentsStorage,
                                               @NonNull SplitEventsManager eventsManager,
                                               @NonNull MySegmentsSyncTaskConfig mySegmentsSyncTaskConfig,
                                               @NonNull MySegmentsUpdateTaskConfig mySegmentsUpdateTaskConfig,
                                               @NonNull MySegmentsOverwriteTaskConfig mySegmentsOverwriteTaskConfig,
                                               @NonNull MySegmentsUpdateTaskConfig myLargeSegmentsUpdateTaskConfig,
                                               @NonNull LoadMySegmentsTaskConfig loadMySegmentsTaskConfig) {
        mHttpFetcher = checkNotNull(httpFetcher);
        mMySegmentsStorage = checkNotNull(storage);
        mMyLargeSegmentsStorage = checkNotNull(myLargeSegmentsStorage);
        mEventsManager = checkNotNull(eventsManager);
        mMySegmentsSyncTaskConfig = checkNotNull(mySegmentsSyncTaskConfig);
        mMySegmentsUpdateTaskConfig = checkNotNull(mySegmentsUpdateTaskConfig);
        mMySegmentsOverwriteTaskConfig = checkNotNull(mySegmentsOverwriteTaskConfig);
        mMyLargeSegmentsUpdateTaskConfig = checkNotNull(myLargeSegmentsUpdateTaskConfig);
        mLoadMySegmentsTaskConfig = checkNotNull(loadMySegmentsTaskConfig);
    }

    @NonNull
    public HttpFetcher<AllSegmentsChange> getHttpFetcher() {
        return mHttpFetcher;
    }

    @NonNull
    public MySegmentsStorage getMySegmentsStorage() {
        return mMySegmentsStorage;
    }

    @NonNull
    public MySegmentsStorage getMyLargeSegmentsStorage() {
        return mMyLargeSegmentsStorage;
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
    public MySegmentsUpdateTaskConfig getMyLargeSegmentsUpdateTaskConfig() {
        return mMyLargeSegmentsUpdateTaskConfig;
    }

    @NonNull
    public LoadMySegmentsTaskConfig getLoadMySegmentsTaskConfig() {
        return mLoadMySegmentsTaskConfig;
    }

    public static MySegmentsTaskFactoryConfiguration get(@NonNull HttpFetcher<AllSegmentsChange> httpFetcher,
                                                         @NonNull MySegmentsStorage mySegmentsStorage,
                                                         @NonNull MySegmentsStorage myLargeSegmentsStorage,
                                                         @NonNull SplitEventsManager eventsManager) {
        return new MySegmentsTaskFactoryConfiguration(httpFetcher,
                mySegmentsStorage,
                myLargeSegmentsStorage,
                eventsManager,
                MySegmentsSyncTaskConfig.get(),
                MySegmentsUpdateTaskConfig.getForMySegments(),
                MySegmentsOverwriteTaskConfig.getForMySegments(),
                MySegmentsUpdateTaskConfig.getForMyLargeSegments(),
                LoadMySegmentsTaskConfig.get());
    }
}
