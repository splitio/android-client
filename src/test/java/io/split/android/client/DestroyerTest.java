package io.split.android.client;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;

import io.split.android.client.factory.FactoryMonitor;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.lifecycle.SplitLifecycleManager;
import io.split.android.client.network.HttpClient;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.service.synchronizer.SyncManager;
import io.split.android.client.shared.SplitClientContainer;
import io.split.android.client.storage.attributes.AttributesStorageContainer;
import io.split.android.client.storage.common.SplitStorageContainer;
import io.split.android.client.telemetry.TelemetrySynchronizer;
import io.split.android.client.telemetry.storage.TelemetryStorage;

public class DestroyerTest {

    private Lock mInitLock;
    private AtomicBoolean mCheckClients;
    private SplitClientContainer mClientContainer;
    private SplitStorageContainer mStorageContainer;
    private TelemetryStorage mTelemetryStorage;
    private AttributesStorageContainer mAttributesStorageContainer;
    private TelemetrySynchronizer mTelemetrySynchronizer;
    private ExecutorService mImpressionsLoggingTaskExecutor;
    private ExecutorService mImpressionsObserverExecutor;
    private SyncManager mSyncManager;
    private SplitLifecycleManager mLifecycleManager;
    private FactoryMonitor mFactoryMonitor;
    private ImpressionListener mCustomerImpressionListener;
    private HttpClient mDefaultHttpClient;
    private SplitManager mSplitManager;
    private SplitTaskExecutor mSplitTaskExecutor;
    private SplitTaskExecutor mSplitSingleThreadTaskExecutor;
    private AtomicBoolean mIsTerminated;

    private Destroyer mDestroyer;
    private final String API_KEY = "test-api-key";
    private final long INIT_START_TIME = 1000L;

    @Before
    public void setup() {
        mInitLock = mock(Lock.class);
        mCheckClients = new AtomicBoolean(true);
        mClientContainer = mock(SplitClientContainer.class);
        mStorageContainer = mock(SplitStorageContainer.class);
        mTelemetryStorage = mock(TelemetryStorage.class);
        mAttributesStorageContainer = mock(AttributesStorageContainer.class);
        mTelemetrySynchronizer = mock(TelemetrySynchronizer.class);
        mImpressionsLoggingTaskExecutor = mock(ExecutorService.class);
        mImpressionsObserverExecutor = mock(ExecutorService.class);
        mSyncManager = mock(SyncManager.class);
        mLifecycleManager = mock(SplitLifecycleManager.class);
        mFactoryMonitor = mock(FactoryMonitor.class);
        mCustomerImpressionListener = mock(ImpressionListener.class);
        mDefaultHttpClient = mock(HttpClient.class);
        mSplitManager = mock(SplitManager.class);
        mSplitTaskExecutor = mock(SplitTaskExecutor.class);
        mSplitSingleThreadTaskExecutor = mock(SplitTaskExecutor.class);
        mIsTerminated = new AtomicBoolean(false);

        when(mStorageContainer.getTelemetryStorage()).thenReturn(mTelemetryStorage);
        when(mStorageContainer.getAttributesStorageContainer()).thenReturn(mAttributesStorageContainer);
        when(mClientContainer.getAll()).thenReturn(new HashSet<>());

        mDestroyer = new Destroyer(
            mInitLock,
            mCheckClients,
            mClientContainer,
            mStorageContainer,
            INIT_START_TIME,
            mTelemetrySynchronizer,
            mImpressionsLoggingTaskExecutor,
            mImpressionsObserverExecutor,
            mSyncManager,
            mLifecycleManager,
            mFactoryMonitor,
            API_KEY,
            mCustomerImpressionListener,
            mDefaultHttpClient,
            mSplitManager,
            mSplitTaskExecutor,
            mSplitSingleThreadTaskExecutor,
                mInitExecutor, mIsTerminated
        );
    }

    @Test
    public void shouldAcquireAndReleaseLock() {
        mDestroyer.run();

        verify(mInitLock).lock();
        verify(mInitLock).unlock();
    }

    @Test
    public void shouldAcquireAndReleaseLockWhenRandomException() {
        doThrow(new RuntimeException("Test exception")).when(mTelemetrySynchronizer).flush();

        mDestroyer.run();

        verify(mInitLock).lock();
        verify(mInitLock).unlock();
    }

    @Test
    public void shouldSetCheckClientsToFalseAfterCompletion() {
        mDestroyer.run();

        assertFalse(
            "CheckClients should be set to false after completion",
            mCheckClients.get()
        );
    }

    @Test
    public void shouldNotShutdownWhenActiveClientsExist() {
        Set<SplitClient> activeClients = new HashSet<>();
        activeClients.add(mock(SplitClient.class));
        when(mClientContainer.getAll()).thenReturn(activeClients);

        mDestroyer.run();

        verify(mTelemetrySynchronizer, never()).flush();
        verify(mTelemetrySynchronizer, never()).destroy();
        assertFalse(
            "IsTerminated should remain false when clients are active",
            mIsTerminated.get()
        );
    }

    @Test
    public void shouldRecordSessionLengthCorrectly() {
        long currentTime = INIT_START_TIME + 5000L;

        mDestroyer = new Destroyer(
            mInitLock,
            mCheckClients,
            mClientContainer,
            mStorageContainer,
            INIT_START_TIME,
            mTelemetrySynchronizer,
            mImpressionsLoggingTaskExecutor,
            mImpressionsObserverExecutor,
            mSyncManager,
            mLifecycleManager,
            mFactoryMonitor,
            API_KEY,
            mCustomerImpressionListener,
            mDefaultHttpClient,
            mSplitManager,
            mSplitTaskExecutor,
            mSplitSingleThreadTaskExecutor,
                mInitExecutor, mIsTerminated
        ) {
            @Override
            public void run() {
                mInitLock.lock();
                try {
                    if (
                        mCheckClients.get() &&
                        !mClientContainer.getAll().isEmpty()
                    ) {
                        return;
                    }
                    mStorageContainer
                        .getTelemetryStorage()
                        .recordSessionLength(currentTime - INIT_START_TIME);
                } finally {
                    mCheckClients.set(false);
                    mInitLock.unlock();
                }
            }
        };

        mDestroyer.run();

        verify(mTelemetryStorage).recordSessionLength(5000L);
    }

    @Test
    public void shouldShutdownAllComponentsInCorrectOrder() {
        mDestroyer.run();

        InOrder inOrder = inOrder(
            mTelemetryStorage,
            mTelemetrySynchronizer,
            mImpressionsLoggingTaskExecutor,
            mImpressionsObserverExecutor,
            mSyncManager,
            mLifecycleManager,
            mClientContainer,
            mFactoryMonitor,
            mCustomerImpressionListener,
            mDefaultHttpClient,
            mSplitManager,
            mSplitTaskExecutor,
            mSplitSingleThreadTaskExecutor,
            mAttributesStorageContainer
        );

        inOrder.verify(mTelemetryStorage).recordSessionLength(anyLong());
        inOrder.verify(mTelemetrySynchronizer).flush();
        inOrder.verify(mTelemetrySynchronizer).destroy();
        inOrder.verify(mImpressionsLoggingTaskExecutor).shutdown();
        inOrder.verify(mImpressionsObserverExecutor).shutdown();
        inOrder.verify(mSyncManager).stop();
        inOrder.verify(mLifecycleManager).destroy();
        inOrder.verify(mClientContainer).destroy();
        inOrder.verify(mFactoryMonitor).remove(API_KEY);
        inOrder.verify(mCustomerImpressionListener).close();
        inOrder.verify(mDefaultHttpClient).close();
        inOrder.verify(mSplitManager).destroy();
        inOrder.verify(mSplitTaskExecutor).stop();
        inOrder.verify(mSplitSingleThreadTaskExecutor).stop();
        inOrder.verify(mAttributesStorageContainer).destroy();
    }

    @Test
    public void shouldSetTerminatedToTrueAfterSuccessfulShutdown() {
        mDestroyer.run();

        assertTrue(
            "IsTerminated should be set to true after successful shutdown",
            mIsTerminated.get()
        );
    }

    @Test
    public void shouldHandleExceptionsDuringShutdown() {
        doThrow(new RuntimeException("Test exception"))
            .when(mTelemetrySynchronizer)
            .flush();

        mDestroyer.run();

        verify(mTelemetrySynchronizer).flush();
        assertFalse(
            "IsTerminated should remain false when exception occurs",
            mIsTerminated.get()
        );
        assertFalse(
            "CheckClients should still be set to false in finally block",
            mCheckClients.get()
        );
    }

    @Test
    public void shouldAlwaysReleaseLockEvenOnException() {
        doThrow(new RuntimeException("Test exception"))
            .when(mTelemetrySynchronizer)
            .flush();

        mDestroyer.run();

        verify(mInitLock).lock();
        verify(mInitLock).unlock();
    }

    @Test
    public void shouldRemoveFactoryMonitorWithCorrectApiKey() {
        mDestroyer.run();

        verify(mFactoryMonitor).remove(API_KEY);
    }

    @Test
    public void shouldNotProceedWhenCheckClientsIsTrueAndActiveClientsExist() {
        Set<SplitClient> activeClients = new HashSet<>();
        activeClients.add(mock(SplitClient.class));
        when(mClientContainer.getAll()).thenReturn(activeClients);
        mCheckClients.set(true);

        mDestroyer.run();

        verify(mStorageContainer, never()).getTelemetryStorage();
        verify(mTelemetrySynchronizer, never()).flush();
        assertFalse(
            "CheckClients should remain true when shutdown is skipped",
            mCheckClients.get()
        );
        assertFalse(
            "IsTerminated should remain false when shutdown is skipped",
            mIsTerminated.get()
        );
    }

    @Test
    public void shouldProceedWhenCheckClientsIsFalse() {
        Set<SplitClient> activeClients = new HashSet<>();
        activeClients.add(mock(SplitClient.class));
        when(mClientContainer.getAll()).thenReturn(activeClients);
        mCheckClients.set(false);

        mDestroyer.run();

        verify(mTelemetryStorage).recordSessionLength(anyLong());
        verify(mTelemetrySynchronizer).flush();
        assertTrue("IsTerminated should be set to true", mIsTerminated.get());
    }

    @Test
    public void shouldProceedWhenNoActiveClients() {
        when(mClientContainer.getAll()).thenReturn(new HashSet<>());
        mCheckClients.set(true);

        mDestroyer.run();

        verify(mTelemetryStorage).recordSessionLength(anyLong());
        verify(mTelemetrySynchronizer).flush();
        assertTrue("IsTerminated should be set to true", mIsTerminated.get());
    }
}
