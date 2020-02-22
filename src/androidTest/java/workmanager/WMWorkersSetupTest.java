package workmanager;

import android.content.Context;

import androidx.arch.core.executor.TaskExecutor;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.testing.TestListenableWorkerBuilder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import io.split.android.client.SplitClientConfig;
import io.split.android.client.service.ServiceConstants;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.impressions.ImpressionsRecorderTask;
import io.split.android.client.service.workmanager.EventsRecorderWorker;
import io.split.android.client.service.workmanager.ImpressionsRecorderWorker;
import io.split.android.client.service.workmanager.MySegmentsSyncWorker;
import io.split.android.client.service.workmanager.SplitsSyncWorker;

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
        Assert.assertEquals(result, dummySuccess("ERROR", SplitTaskType.SPLITS_SYNC));
    }

    @Test
    public void mySegmentsSyncWorker() throws Exception {
        ListenableWorker worker =
                TestListenableWorkerBuilder.from(mContext, MySegmentsSyncWorker.class)
                        .setInputData(buildInputData(
                                new Data.Builder().putString(
                                        ServiceConstants.WORKER_PARAM_KEY, "key").build()))
                        .build();

        ListenableWorker.Result result = worker.startWork().get();
        Assert.assertEquals(result, dummySuccess("ERROR", SplitTaskType.MY_SEGMENTS_SYNC));
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
        Assert.assertEquals(result, dummySuccess("SUCCESS", SplitTaskType.EVENTS_RECORDER));
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
        Assert.assertEquals(result, dummySuccess("SUCCESS", SplitTaskType.IMPRESSIONS_RECORDER));
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

    private ListenableWorker.Result dummySuccess(String result, SplitTaskType taskType) {
        Data data = new Data.Builder()
                .putString(ServiceConstants.TASK_INFO_FIELD_STATUS, result)
                .putString(ServiceConstants.TASK_INFO_FIELD_TYPE, taskType.toString())
                .putInt(ServiceConstants.TASK_INFO_FIELD_RECORDS_NON_SENT, 0)
                .putLong(ServiceConstants.TASK_INFO_FIELD_BYTES_NON_SET, 0)
                .build();
        return ListenableWorker.Result.success(data);
    }
}