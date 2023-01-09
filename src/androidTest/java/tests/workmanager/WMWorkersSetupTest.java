package tests.workmanager;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.testing.TestListenableWorkerBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.workmanager.EventsRecorderWorker;
import io.split.android.client.service.workmanager.ImpressionsRecorderWorker;
import io.split.android.client.service.workmanager.MySegmentsSyncWorker;
import io.split.android.client.service.workmanager.SplitsSyncWorker;
import io.split.android.client.service.workmanager.UniqueKeysRecorderWorker;

public class WMWorkersSetupTest {

    private Context mContext;

    @Before
    public void setUp() {
        mContext = ApplicationProvider.getApplicationContext();
        SplitClientConfig.splitSdkVersion = "testversion";
    }

    @Test
    public void splitSyncWorker() throws Exception {
        ListenableWorker worker =
                TestListenableWorkerBuilder.from(mContext, SplitsSyncWorker.class)
                        .setInputData(buildInputData(null))
                        .build();

        ListenableWorker.Result result = worker.startWork().get();
        Assert.assertEquals(result, dummySuccess());
    }

    @Test
    public void mySegmentsSyncWorker() throws Exception {
        String[] keys = {"key1", "key2"};
        ListenableWorker worker =
                TestListenableWorkerBuilder.from(mContext, MySegmentsSyncWorker.class)
                        .setInputData(buildInputData(
                                new Data.Builder().putStringArray(
                                        ServiceConstants.WORKER_PARAM_KEY, keys).build()))
                        .build();

        ListenableWorker.Result result = worker.startWork().get();
        Assert.assertEquals(result, dummySuccess());
    }

    @Test
    public void eventsRecorderWorker() throws Exception {
        ListenableWorker worker =
                TestListenableWorkerBuilder.from(mContext, EventsRecorderWorker.class)
                        .setInputData(buildInputData(
                                new Data.Builder().putInt(
                                        ServiceConstants.WORKER_PARAM_EVENTS_PER_PUSH, 10).build()))
                        .build();

        ListenableWorker.Result result = worker.startWork().get();
        Assert.assertEquals(result, dummySuccess());
    }

    @Test
    public void impressionsRecorderWorker() throws Exception {
        ListenableWorker worker =
                TestListenableWorkerBuilder.from(mContext, ImpressionsRecorderWorker.class)
                        .setInputData(buildInputData(
                                new Data.Builder().putInt(
                                        ServiceConstants.WORKER_PARAM_IMPRESSIONS_PER_PUSH,
                                        10).build()))
                        .build();

        ListenableWorker.Result result = worker.startWork().get();
        Assert.assertEquals(result, dummySuccess());
    }

    @Test
    public void uniqueKeysRecorderWorker() throws Exception {
        ListenableWorker worker =
                TestListenableWorkerBuilder.from(mContext, UniqueKeysRecorderWorker.class)
                        .setInputData(buildInputData(
                                new Data.Builder()
                                        .putInt(ServiceConstants.WORKER_PARAM_UNIQUE_KEYS_PER_PUSH, 10)
                                        .putLong(ServiceConstants.WORKER_PARAM_UNIQUE_KEYS_ESTIMATED_SIZE_IN_BYTES, 1000L)
                                        .build()))
                        .build();

        ListenableWorker.Result result = worker.startWork().get();
        Assert.assertEquals(result, dummySuccess());
    }

    private Data buildInputData(Data customData) {
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_DATABASE_NAME, "thedatabase");
        dataBuilder.putString(ServiceConstants.WORKER_PARAM_API_KEY, "thisapikey");
        dataBuilder.putString(
                ServiceConstants.WORKER_PARAM_EVENTS_ENDPOINT, "http://events.com/");
        if (customData != null) {
            dataBuilder.putAll(customData);
        }
        return dataBuilder.build();
    }

    private ListenableWorker.Result dummySuccess() {
        return ListenableWorker.Result.success();
    }
}
