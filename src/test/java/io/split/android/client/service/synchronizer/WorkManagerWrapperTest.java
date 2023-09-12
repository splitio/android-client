package io.split.android.client.service.synchronizer;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.impl.model.WorkSpec;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import io.split.android.client.ServiceEndpoints;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFilter;
import io.split.android.client.service.executor.SplitTaskType;
import io.split.android.client.service.workmanager.EventsRecorderWorker;
import io.split.android.client.service.workmanager.ImpressionsRecorderWorker;
import io.split.android.client.service.workmanager.MySegmentsSyncWorker;
import io.split.android.client.service.workmanager.SplitsSyncWorker;

public class WorkManagerWrapperTest {

    @Mock
    private WorkManager mWorkManager;
    private WorkManagerWrapper mWrapper;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        SplitClientConfig splitClientConfig = new SplitClientConfig.Builder()
                .serviceEndpoints(
                        ServiceEndpoints.builder()
                                .sseAuthServiceEndpoint("https://test.split.io/serviceEndpoint")
                                .apiEndpoint("https://test.split.io/api")
                                .eventsEndpoint("https://test.split.io/events")
                                .telemetryServiceEndpoint("https://test.split.io/telemetry")
                                .build()
                )
                .synchronizeInBackgroundPeriod(5263)
                .eventsPerPush(526)
                .impressionsPerPush(256)
                .backgroundSyncWhenWifiOnly(true)
                .backgroundSyncWhenBatteryNotLow(false)
                .build();

        try {
            Method method = splitClientConfig.getClass().getDeclaredMethod("enableTelemetry");
            method.setAccessible(true);
            method.invoke(splitClientConfig);
        } catch (Exception exception) {
            exception.printStackTrace();
        }

        mWrapper = new WorkManagerWrapper(
                mWorkManager,
                splitClientConfig,
                "api_key",
                "test_database_name",
                SplitFilter.bySet(Arrays.asList("set_1", "set_2"))
        );
    }

    @Test
    public void removeWorkCancelsJobs() {
        mWrapper.removeWork();

        verify(mWorkManager).cancelUniqueWork(SplitTaskType.SPLITS_SYNC.toString());
        verify(mWorkManager).cancelUniqueWork(SplitTaskType.MY_SEGMENTS_SYNC.toString());
        verify(mWorkManager).cancelUniqueWork(SplitTaskType.EVENTS_RECORDER.toString());
        verify(mWorkManager).cancelUniqueWork(SplitTaskType.IMPRESSIONS_RECORDER.toString());
    }

    @Test
    public void scheduleWorkSchedulesSplitsJob() {
        mWrapper.scheduleWork();

        Data inputData = new Data.Builder()
                .putLong("splitCacheExpiration", 864000)
                .putString("endpoint", "https://test.split.io/api")
                .putBoolean("shouldRecordTelemetry", true)
                .putStringArray("configuredFilterValues", new String[]{"set_1", "set_2"})
                .putString("configuredFilterType", SplitFilter.Type.BY_SET.queryStringField())
                .build();

        PeriodicWorkRequest expectedRequest = new PeriodicWorkRequest
                .Builder(SplitsSyncWorker.class, 5263, TimeUnit.MINUTES)
                .setInputData(buildInputData(inputData))
                .setConstraints(buildConstraints())
                .setInitialDelay(15L, TimeUnit.MINUTES)
                .build();

        ArgumentCaptor<PeriodicWorkRequest> argumentCaptor = ArgumentCaptor.forClass(PeriodicWorkRequest.class);

        verify(mWorkManager).enqueueUniquePeriodicWork(
                eq(SplitTaskType.SPLITS_SYNC.toString()),
                eq(ExistingPeriodicWorkPolicy.REPLACE),
                argumentCaptor.capture()
        );

        assertWorkSpecMatches(argumentCaptor.getValue().getWorkSpec(), expectedRequest.getWorkSpec());
    }

    @Test
    public void scheduleWorkSchedulesEventsJob() {
        mWrapper.scheduleWork();

        Data inputData = new Data.Builder()
                .putString("endpoint", "https://test.split.io/events")
                .putInt("eventsPerPush", 526)
                .putBoolean("shouldRecordTelemetry", true)
                .build();

        PeriodicWorkRequest expectedRequest = new PeriodicWorkRequest
                .Builder(EventsRecorderWorker.class, 5263, TimeUnit.MINUTES)
                .setInputData(buildInputData(inputData))
                .setConstraints(buildConstraints())
                .setInitialDelay(15L, TimeUnit.MINUTES)
                .build();

        ArgumentCaptor<PeriodicWorkRequest> argumentCaptor = ArgumentCaptor.forClass(PeriodicWorkRequest.class);

        verify(mWorkManager).enqueueUniquePeriodicWork(
                eq(SplitTaskType.EVENTS_RECORDER.toString()),
                eq(ExistingPeriodicWorkPolicy.REPLACE),
                argumentCaptor.capture()
        );

        assertWorkSpecMatches(argumentCaptor.getValue().getWorkSpec(), expectedRequest.getWorkSpec());
    }

    @Test
    public void scheduleWorkSchedulesImpressionsJob() {
        mWrapper.scheduleWork();

        Data inputData = new Data.Builder()
                .putString("endpoint", "https://test.split.io/events")
                .putInt("impressionsPerPush", 256)
                .putBoolean("shouldRecordTelemetry", true).build();

        PeriodicWorkRequest expectedRequest = new PeriodicWorkRequest
                .Builder(ImpressionsRecorderWorker.class, 5263, TimeUnit.MINUTES)
                .setInputData(buildInputData(inputData))
                .setConstraints(buildConstraints())
                .setInitialDelay(15L, TimeUnit.MINUTES)
                .build();

        ArgumentCaptor<PeriodicWorkRequest> argumentCaptor = ArgumentCaptor.forClass(PeriodicWorkRequest.class);

        verify(mWorkManager).enqueueUniquePeriodicWork(
                eq(SplitTaskType.IMPRESSIONS_RECORDER.toString()),
                eq(ExistingPeriodicWorkPolicy.REPLACE),
                argumentCaptor.capture()
        );

        assertWorkSpecMatches(argumentCaptor.getValue().getWorkSpec(), expectedRequest.getWorkSpec());
    }

    @Test
    public void scheduleMySegmentsWorkSchedulesJob() {
        HashSet<String> keys = new HashSet<>();
        keys.add("key1");
        keys.add("key2");
        mWrapper.scheduleMySegmentsWork(keys);

        Data.Builder dataBuilder = new Data.Builder();
        String[] keysArray = new String[keys.size()];
        keys.toArray(keysArray);
        dataBuilder.putString("endpoint", "https://test.split.io/api");
        dataBuilder.putStringArray("key", keysArray);
        dataBuilder.putBoolean("shouldRecordTelemetry", true);

        PeriodicWorkRequest expectedRequest = new PeriodicWorkRequest
                .Builder(MySegmentsSyncWorker.class, 5263, TimeUnit.MINUTES)
                .setInputData(buildInputData(dataBuilder.build()))
                .setConstraints(buildConstraints())
                .setInitialDelay(15L, TimeUnit.MINUTES)
                .build();

        ArgumentCaptor<PeriodicWorkRequest> argumentCaptor = ArgumentCaptor.forClass(PeriodicWorkRequest.class);

        verify(mWorkManager).enqueueUniquePeriodicWork(
                eq(SplitTaskType.MY_SEGMENTS_SYNC.toString()),
                eq(ExistingPeriodicWorkPolicy.REPLACE),
                argumentCaptor.capture()
        );

        assertWorkSpecMatches(argumentCaptor.getValue().getWorkSpec(), expectedRequest.getWorkSpec());
    }

    private void assertWorkSpecMatches(WorkSpec workSpec, WorkSpec expectedWorkSpec) {
        assertEquals(expectedWorkSpec.backoffPolicy, workSpec.backoffPolicy);
        assertEquals(expectedWorkSpec.backoffDelayDuration, workSpec.backoffDelayDuration);
        assertEquals(expectedWorkSpec.constraints, workSpec.constraints);
        assertEquals(expectedWorkSpec.initialDelay, workSpec.initialDelay);
        assertEquals(expectedWorkSpec.input, workSpec.input);
        assertEquals(expectedWorkSpec.minimumRetentionDuration, workSpec.minimumRetentionDuration);
        assertEquals(expectedWorkSpec.periodStartTime, workSpec.periodStartTime);
        assertEquals(expectedWorkSpec.workerClassName, workSpec.workerClassName);
        assertEquals(expectedWorkSpec.isBackedOff(), workSpec.isBackedOff());
        assertEquals(expectedWorkSpec.isPeriodic(), workSpec.isPeriodic());
    }

    private Data buildInputData(Data customData) {
        Data.Builder dataBuilder = new Data.Builder();
        dataBuilder.putString("databaseName", "test_database_name");
        dataBuilder.putString("apiKey", "api_key");
        dataBuilder.putBoolean("encryptionEnabled", false);
        if (customData != null) {
            dataBuilder.putAll(customData);
        }
        return dataBuilder.build();
    }

    private Constraints buildConstraints() {
        Constraints.Builder constraintsBuilder = new Constraints.Builder();
        constraintsBuilder.setRequiredNetworkType(NetworkType.UNMETERED);
        constraintsBuilder.setRequiresBatteryNotLow(false);
        return constraintsBuilder.build();
    }
}
