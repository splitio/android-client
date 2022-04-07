package io.split.android.client.service.synchronizer.attributes;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.events.SplitEventsManager;
import io.split.android.client.service.attributes.AttributeTaskFactory;
import io.split.android.client.service.attributes.LoadAttributesTask;
import io.split.android.client.service.executor.SplitTaskExecutor;
import io.split.android.client.storage.attributes.PersistentAttributesStorage;

public class AttributesSynchronizerImplTest {

    @Mock
    private SplitTaskExecutor mSplitTaskExecutor;
    @Mock
    private SplitEventsManager mSplitEventsManager;
    @Mock
    private AttributeTaskFactory mAttributeTaskFactory;
    @Mock
    private PersistentAttributesStorage mPersistentAttributeStorage;
    private AttributesSynchronizerImpl mSynchronizer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mSynchronizer = new AttributesSynchronizerImpl(mSplitTaskExecutor, mSplitEventsManager, mAttributeTaskFactory, mPersistentAttributeStorage);
    }

    @Test
    public void loadLocalAttributesSubmitsTaskInTaskExecutor() {
        LoadAttributesTask task = mock(LoadAttributesTask.class);
        when(mAttributeTaskFactory.createLoadAttributesTask(mPersistentAttributeStorage)).thenReturn(task);

        mSynchronizer.loadAttributesFromCache();

        verify(mSplitTaskExecutor).submit(eq(task), any());
    }
}
