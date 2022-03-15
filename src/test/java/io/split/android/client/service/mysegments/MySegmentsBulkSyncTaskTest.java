package io.split.android.client.service.mysegments;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class MySegmentsBulkSyncTaskTest {

    @Test
    public void allTasksAreExecuted() {
        Set<MySegmentsSyncTask> taskSet = new HashSet<>();
        for (int i = 0; i < 5; i++) {
            taskSet.add(mock(MySegmentsSyncTask.class));
        }

        MySegmentsBulkSyncTask mTask = new MySegmentsBulkSyncTask(taskSet);
        mTask.execute();

        for (MySegmentsSyncTask subTask : taskSet) {
            verify(subTask).execute();
        }
    }
}
