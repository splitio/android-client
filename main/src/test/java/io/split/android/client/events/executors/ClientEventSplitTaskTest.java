package io.split.android.client.events.executors;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import io.split.android.client.SplitClient;
import io.split.android.client.events.SplitEventTask;

public class ClientEventSplitTaskTest {

    @Mock
    private SplitEventTask mTask;
    @Mock
    private SplitClient mClient;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void onPostExecutionViewIsExecutedWhenIsMainThread() {
        ClientEventSplitTask clientEventSplitTask = new ClientEventSplitTask(mTask, mClient, true);

        clientEventSplitTask.execute();

        verify(mTask).onPostExecutionView(mClient);
        verify(mTask, times(0)).onPostExecution(mClient);
    }

    @Test
    public void onPostExecutionIsExecutedWhenIsNotMainThread() {
        ClientEventSplitTask clientEventSplitTask = new ClientEventSplitTask(mTask, mClient, false);

        clientEventSplitTask.execute();

        verify(mTask).onPostExecution(mClient);
        verify(mTask, times(0)).onPostExecutionView(mClient);
    }
}
