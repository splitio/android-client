package io.split.android.client.service.workmanager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import io.split.android.client.network.HttpClient;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.storage.db.SplitRoomDatabase;

public abstract class SplitWorker extends Worker {

    private final SplitRoomDatabase mDatabase;
    private final HttpClient mHttpClient;
    private final String mEndpoint;

    protected SplitTask mSplitTask;

    public SplitWorker(@NonNull Context context,
                       @NonNull WorkerParameters workerParams) {

        super(context, workerParams);
        Data inputData = workerParams.getInputData();
        String databaseName = inputData.getString(ServiceConstants.WORKER_PARAM_DATABASE_NAME);
        String apiKey = inputData.getString(ServiceConstants.WORKER_PARAM_API_KEY);
        mEndpoint = inputData.getString(ServiceConstants.WORKER_PARAM_ENDPOINT);
        mDatabase = SplitRoomDatabase.getDatabase(context, databaseName);
        mHttpClient = HttpClientProvider.buildHttpClient(apiKey,
                inputData.getString(ServiceConstants.WORKER_PARAM_CERTIFICATE_PINS),
                inputData.getBoolean(ServiceConstants.WORKER_PARAM_USES_PROXY, false), mDatabase);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (mSplitTask != null) {
            mSplitTask.execute();
            return Result.success();
        } else {
            return Result.failure();
        }
    }

    protected SplitRoomDatabase getDatabase() {
        return mDatabase;
    }

    public HttpClient getHttpClient() {
        return mHttpClient;
    }

    public String getEndPoint() {
        return mEndpoint;
    }
}
