package io.split.android.client.events.executors;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Objects;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutor;

public class SplitEventExecutorWithClientTest {

    @Mock
    private SplitTaskExecutor mSplitTaskExecutor;
    @Mock
    private SplitEventTask mSplitEventTask;
    @Mock
    private SplitClient mSplitClient;

    private SplitEventExecutorWithClient mSplitEventExecutorWithClient;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);

        mSplitEventExecutorWithClient = new SplitEventExecutorWithClient(mSplitTaskExecutor, mSplitEventTask, mSplitClient);
    }

    @Test
    public void executeSubmitsBothTasksToTaskExecutor() {
        mSplitEventExecutorWithClient.execute();

        verify(mSplitTaskExecutor).submit(argThat(BackgroundSplitTask.class::isInstance), argThat(Objects::isNull));
        verify(mSplitTaskExecutor).submitOnMainThread(argThat(MainThreadSplitTask.class::isInstance));
    }
}
