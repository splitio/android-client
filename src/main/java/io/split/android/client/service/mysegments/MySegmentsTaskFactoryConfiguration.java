package io.split.android.client.service.mysegments;

import static com.google.common.base.Preconditions.checkNotNull;

import androidx.annotation.NonNull;

import java.util.List;

import io.split.android.client.dtos.MySegment;
import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.storage.mysegments.MySegmentsStorage;

public class MySegmentsTaskFactoryConfiguration {

    private final HttpFetcher<List<MySegment>> mHttpFetcher;
    private final MySegmentsStorage mStorage;
    private final SplitEventsManager mEventsManager;

    public MySegmentsTaskFactoryConfiguration(@NonNull HttpFetcher<List<MySegment>> httpFetcher,
                                              @NonNull MySegmentsStorage storage,
                                              @NonNull SplitEventsManager eventsManager) {
        mHttpFetcher = checkNotNull(httpFetcher);
        mStorage = checkNotNull(storage);
        mEventsManager = checkNotNull(eventsManager);
    }

    @NonNull
    public HttpFetcher<List<MySegment>> getHttpFetcher() {
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
}
