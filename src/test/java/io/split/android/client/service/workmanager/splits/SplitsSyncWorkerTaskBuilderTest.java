package io.split.android.client.service.workmanager.splits;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import androidx.annotation.NonNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.net.URISyntaxException;

import io.split.android.client.dtos.TargetingRulesChange;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.http.HttpFetcher;
import io.split.android.client.service.rules.RuleBasedSegmentChangeProcessor;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.service.splits.SplitsSyncHelper;
import io.split.android.client.service.splits.SplitsSyncTask;
import io.split.android.client.storage.general.GeneralInfoStorage;
import io.split.android.client.storage.rbs.RuleBasedSegmentStorageProducer;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.telemetry.storage.TelemetryStorage;

public class SplitsSyncWorkerTaskBuilderTest {

    private StorageProvider mStorageProvider;
    private FetcherProvider mFetcherProvider;
    private SplitChangeProcessor mSplitChangeProcessor;
    private RuleBasedSegmentChangeProcessor mRuleBasedSegmentChangeProcessor;
    private SyncHelperProvider mSplitsSyncHelperProvider;
    private SplitsStorage mSplitsStorage;
    private HttpFetcher<TargetingRulesChange> mSplitsFetcher;
    private TelemetryStorage mTelemetryStorage;
    private RuleBasedSegmentStorageProducer mRuleBasedSegmentStorageProducer;
    private GeneralInfoStorage mGeneralinfoStorage;

    @Before
    public void setUp() throws URISyntaxException {
        mStorageProvider = mock(StorageProvider.class);
        mFetcherProvider = mock(FetcherProvider.class);
        mSplitChangeProcessor = mock(SplitChangeProcessor.class);
        mSplitsStorage = mock(SplitsStorage.class);
        mSplitsFetcher = mock(HttpFetcher.class);
        mTelemetryStorage = mock(TelemetryStorage.class);
        mSplitsSyncHelperProvider = mock(SyncHelperProvider.class);
        mRuleBasedSegmentStorageProducer = mock(RuleBasedSegmentStorageProducer.class);
        mRuleBasedSegmentChangeProcessor = mock(RuleBasedSegmentChangeProcessor.class);
        mGeneralinfoStorage = mock(GeneralInfoStorage.class);

        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn("filterQueryString");
        when(mStorageProvider.provideSplitsStorage()).thenReturn(mSplitsStorage);
        when(mStorageProvider.provideRuleBasedSegmentStorage()).thenReturn(mRuleBasedSegmentStorageProducer);
        when(mStorageProvider.provideTelemetryStorage()).thenReturn(mTelemetryStorage);
        when(mFetcherProvider.provideFetcher("filterQueryString")).thenReturn(mSplitsFetcher);
        when(mSplitsSyncHelperProvider.provideSplitsSyncHelper(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(mock(SplitsSyncHelper.class));
    }

    @Test
    public void getTaskUsesStorageProviderForSplitsStorage() {
        SplitsSyncWorkerTaskBuilder builder = getSplitsSyncWorkerTaskBuilder(null);

        builder.getTask();

        verify(mStorageProvider).provideSplitsStorage();
    }

    @Test
    public void getTaskUsesFetcherProviderForFetcher() throws URISyntaxException {
        SplitsSyncWorkerTaskBuilder builder = getSplitsSyncWorkerTaskBuilder(null);

        builder.getTask();

        verify(mFetcherProvider).provideFetcher(any());
    }

    @Test
    public void getTaskUsesStorageProviderForTelemetryStorage() {
        SplitsSyncWorkerTaskBuilder builder = new SplitsSyncWorkerTaskBuilder(
                mStorageProvider,
                mFetcherProvider,
                mSplitChangeProcessor,
                mRuleBasedSegmentChangeProcessor,
                mSplitsSyncHelperProvider,
                null);

        builder.getTask();

        verify(mStorageProvider).provideTelemetryStorage();
    }

    @Test
    public void getTaskUsesSplitsSyncHelperProviderForSplitsSyncHelper() throws URISyntaxException {
        when(mSplitsStorage.getSplitsFilterQueryString()).thenReturn("string");
        when(mStorageProvider.provideRuleBasedSegmentStorage()).thenReturn(mRuleBasedSegmentStorageProducer);
        when(mFetcherProvider.provideFetcher("string")).thenReturn(mSplitsFetcher);
        when(mStorageProvider.provideGeneralInfoStorage()).thenReturn(mGeneralinfoStorage);

        SplitsSyncWorkerTaskBuilder builder = new SplitsSyncWorkerTaskBuilder(
                mStorageProvider,
                mFetcherProvider,
                mSplitChangeProcessor,
                mRuleBasedSegmentChangeProcessor,
                mSplitsSyncHelperProvider,
                "1.5");

        builder.getTask();

        verify(mSplitsStorage).getSplitsFilterQueryString();
        verify(mFetcherProvider).provideFetcher("string");
        verify(mSplitsSyncHelperProvider).provideSplitsSyncHelper(mSplitsFetcher,
                mSplitsStorage,
                mSplitChangeProcessor,
                mRuleBasedSegmentChangeProcessor,
                mRuleBasedSegmentStorageProducer,
                mGeneralinfoStorage,
                mTelemetryStorage,
                "1.5");
    }

    @Test
    public void getTaskReturnsNullWhenURISyntaxExceptionIsThrown() throws URISyntaxException {
        when(mFetcherProvider.provideFetcher("filterQueryString")).thenThrow(new URISyntaxException("test", "test"));

        SplitsSyncWorkerTaskBuilder builder = getSplitsSyncWorkerTaskBuilder(null);

        SplitTask task = builder.getTask();

        assertNull(task);
    }

    @Test
    public void getTaskUsesSplitSyncTaskStaticMethod() {
        try (MockedStatic<SplitsSyncTask> mockedStatic = mockStatic(SplitsSyncTask.class)) {
            SplitsSyncHelper splitsSyncHelper = mock(SplitsSyncHelper.class);
            when(mSplitsSyncHelperProvider.provideSplitsSyncHelper(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(splitsSyncHelper);
            when(mStorageProvider.provideRuleBasedSegmentStorage()).thenReturn(mRuleBasedSegmentStorageProducer);

            SplitsSyncWorkerTaskBuilder builder = getSplitsSyncWorkerTaskBuilder("2.5");

            builder.getTask();

            mockedStatic.verify(() -> SplitsSyncTask.buildForBackground(splitsSyncHelper, mSplitsStorage, mRuleBasedSegmentStorageProducer, "filterQueryString", mTelemetryStorage));
        }
    }

    @NonNull
    private SplitsSyncWorkerTaskBuilder getSplitsSyncWorkerTaskBuilder(String flagsSpec) {
        return new SplitsSyncWorkerTaskBuilder(
                mStorageProvider,
                mFetcherProvider,
                mSplitChangeProcessor,
                mRuleBasedSegmentChangeProcessor,
                mSplitsSyncHelperProvider,
                flagsSpec);
    }
}
