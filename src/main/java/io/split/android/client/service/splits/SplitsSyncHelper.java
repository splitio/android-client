package io.split.android.client.service.splits;

import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

import io.split.android.client.dtos.SplitChange;
import io.split.android.client.network.SplitHttpHeadersBuilder;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.http.HttpFetcherException;
import io.split.android.client.service.sseclient.ReconnectBackoffCounter;
import io.split.android.client.storage.splits.ProcessedSplitChange;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.utils.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.split.android.client.service.executor.SplitTaskExecutionInfo.HTTP_STATUS;

public class SplitsSyncHelper {

    private final HttpFetcher<SplitChange> mSplitFetcher;
    private final SplitsStorage mSplitsStorage;
    private final SplitChangeProcessor mSplitChangeProcessor;

    public SplitsSyncHelper(HttpFetcher<SplitChange> splitFetcher,
                            SplitsStorage splitsStorage,
                            SplitChangeProcessor splitChangeProcessor) {
        mSplitFetcher = checkNotNull(splitFetcher);
        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitChangeProcessor = checkNotNull(splitChangeProcessor);
    }

    public SplitTaskExecutionInfo sync(Map<String, Object> params,
                                       boolean clearBeforeUpdate,
                                       boolean avoidCache) {
        try {
            SplitChange splitChange = mSplitFetcher.execute(params, getHeaders(avoidCache));
            if (clearBeforeUpdate) {
                mSplitsStorage.clear();
            }
            mSplitsStorage.update(mSplitChangeProcessor.process(splitChange));
        } catch (HttpFetcherException e) {
            logError("Network error while fetching splits" + e.getLocalizedMessage());

            return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC, getHttpStatusMap(e));
        } catch (Exception e) {
            logError("Unexpected while fetching splits" + e.getLocalizedMessage());
            return SplitTaskExecutionInfo.error(SplitTaskType.SPLITS_SYNC);
        }
        Logger.d("Features have been updated");
        return SplitTaskExecutionInfo.success(SplitTaskType.SPLITS_SYNC);
    }

    public boolean cacheHasExpired(long storedChangeNumber, long updateTimestamp, long cacheExpirationInSeconds) {
        long elepased = now() - updateTimestamp;
        return storedChangeNumber > -1
                && updateTimestamp > 0
                && (elepased > cacheExpirationInSeconds);
    }

    private Map<String, Object> getHttpStatusMap(HttpFetcherException exception) {
        return Collections.singletonMap(HTTP_STATUS, exception.getHttpStatus());
    }

    private long now() {
        return System.currentTimeMillis() / 1000;
    }

    private void logError(String message) {
        Logger.e("Error while executing splits sync/update task: " + message);
    }

    private @Nullable Map<String, String> getHeaders(boolean avoidCache) {
        if (avoidCache) {
            return SplitHttpHeadersBuilder.noCacheHeaders();
        }
        return null;
    }
}
