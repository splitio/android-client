package io.split.android.client.events.executors;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Objects;

import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.service.executor.SplitTaskExecutor;

public class SplitEventExecutorFactoryTest {

    @Mock
    private SplitEventExecutorResources mSplitEventExecutorResources;
    @Mock
    private SplitEventTask mSplitEventTask;
    @Mock
    private SplitTaskExecutor mSplitTaskExecutor;

    @Before
    public void setup() {
         MockitoAnnotations.openMocks(this);
    }

    @Test
    public void factoryReturnsWorkingExecutor() {

        SplitEventExecutorAbstract executor = SplitEventExecutorFactory.factory(mSplitTaskExecutor, SplitEvent.SDK_READY, mSplitEventTask, mSplitEventExecutorResources);

        executor.execute();

        verify(mSplitTaskExecutor).submit(argThat(BackgroundSplitTask.class::isInstance), argThat(Objects::isNull));
        verify(mSplitTaskExecutor).submitOnMainThread(argThat(MainThreadSplitTask.class::isInstance));
    }
}
