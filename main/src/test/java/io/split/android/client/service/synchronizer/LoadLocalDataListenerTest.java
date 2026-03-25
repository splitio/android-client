package io.split.android.client.service.synchronizer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import io.split.android.client.events.metadata.EventMetadata;
import io.split.android.client.events.ISplitEventsManager;
import io.split.android.client.events.SplitInternalEvent;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.SplitTaskType;

public class LoadLocalDataListenerTest {

    private ISplitEventsManager mEventsManager;

    @Before
    public void setUp() {
        mEventsManager = mock(ISplitEventsManager.class);
    }

    @Test
    public void taskExecutedSuccessFiresEventWithoutMetadataWhenProviderIsNull() {
        LoadLocalDataListener listener = new LoadLocalDataListener(
                mEventsManager, SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);

        listener.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_SPLITS));

        verify(mEventsManager).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE), isNull());
    }

    @Test
    public void taskExecutedSuccessFiresEventWithMetadataWhenProviderIsNotNull() {
        EventMetadata mockMetadata = mock(EventMetadata.class);
        LoadLocalDataListener.MetadataProvider provider = () -> mockMetadata;

        LoadLocalDataListener listener = new LoadLocalDataListener(
                mEventsManager, SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE, provider);

        listener.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_SPLITS));

        verify(mEventsManager).notifyInternalEvent(eq(SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE), eq(mockMetadata));
    }

    @Test
    public void taskExecutedErrorDoesNotFireEvent() {
        LoadLocalDataListener listener = new LoadLocalDataListener(
                mEventsManager, SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE);

        listener.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.LOAD_LOCAL_SPLITS));

        verify(mEventsManager, never()).notifyInternalEvent(any(), any());
    }

    @Test
    public void metadataProviderIsCalledWhenTaskSucceeds() {
        LoadLocalDataListener.MetadataProvider provider = mock(LoadLocalDataListener.MetadataProvider.class);
        when(provider.getMetadata()).thenReturn(null);

        LoadLocalDataListener listener = new LoadLocalDataListener(
                mEventsManager, SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE, provider);

        listener.taskExecuted(SplitTaskExecutionInfo.success(SplitTaskType.LOAD_LOCAL_SPLITS));

        verify(provider).getMetadata();
    }

    @Test
    public void metadataProviderIsNotCalledWhenTaskFails() {
        LoadLocalDataListener.MetadataProvider provider = mock(LoadLocalDataListener.MetadataProvider.class);

        LoadLocalDataListener listener = new LoadLocalDataListener(
                mEventsManager, SplitInternalEvent.SPLITS_LOADED_FROM_STORAGE, provider);

        listener.taskExecuted(SplitTaskExecutionInfo.error(SplitTaskType.LOAD_LOCAL_SPLITS));

        verify(provider, never()).getMetadata();
    }
}
