package io.split.android.client.localhost;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import javax.security.auth.Destroyable;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.lifecycle.SplitLifecycleAware;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.splits.LoadSplitsTask;
import io.split.android.client.storage.splits.SplitsStorage;

public class LocalhostSynchronizer implements SplitLifecycleAware, Destroyable {
    private final SplitTaskExecutor mTaskExecutor;
    private final int mRefreshRate;
    private final SplitsStorage mSplitsStorage;
    private final String mSplitsFilterQueryStringFromConfig;
    private final String mFlagsSpecFromConfig;

    public LocalhostSynchronizer(@NonNull SplitTaskExecutor taskExecutor,
                                 @NonNull SplitClientConfig splitClientConfig,
                                 @NonNull SplitsStorage splitsStorage,
                                 @Nullable String splitsFilterQueryStringFromConfig,
                                 @Nullable String flagsSpecFromConfig) {
        mTaskExecutor = checkNotNull(taskExecutor);
        mRefreshRate = checkNotNull(splitClientConfig).offlineRefreshRate();
        mSplitsStorage = checkNotNull(splitsStorage);
        mSplitsFilterQueryStringFromConfig = splitsFilterQueryStringFromConfig;
        mFlagsSpecFromConfig = flagsSpecFromConfig;
    }

    public void start() {
        SplitTask loadTask = new LoadSplitsTask(mSplitsStorage, mSplitsFilterQueryStringFromConfig, mFlagsSpecFromConfig);
        if (mRefreshRate > 0) {
            mTaskExecutor.schedule(
                    loadTask, 0,
                    mRefreshRate, null);
        } else {
            mTaskExecutor.submit(loadTask, null);
        }
    }

    public void pause() {
        mTaskExecutor.pause();
    }

    public void resume() {
        mTaskExecutor.resume();
    }

    public void stop() {
        mTaskExecutor.stop();
    }
}
