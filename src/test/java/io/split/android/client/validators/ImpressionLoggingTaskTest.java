package io.split.android.client.validators;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;

import io.split.android.client.impressions.Impression;
import io.split.android.client.impressions.ImpressionListener;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.executor.SplitTaskType;

public class ImpressionLoggingTaskTest {

    private ImpressionListener mImpressionListener;
    private ImpressionLoggingTask mImpressionsLoggingTask;

    @Before
    public void setUp() {
        mImpressionListener = mock(ImpressionListener.class);
    }

    @Test
    public void executeLogsImpressionInListener() {
        Impression impression = createImpression();
        mImpressionsLoggingTask = new ImpressionLoggingTask(mImpressionListener, impression);

        mImpressionsLoggingTask.execute();

        verify(mImpressionListener).log(impression);
    }

    @Test
    public void successfulExecutionReturnsSuccessInfo() {
        Impression impression = createImpression();
        mImpressionsLoggingTask = new ImpressionLoggingTask(mImpressionListener, impression);

        SplitTaskExecutionInfo result = mImpressionsLoggingTask.execute();

        assertEquals(result.getStatus(), SplitTaskExecutionStatus.SUCCESS);
        assertEquals(result.getTaskType(), SplitTaskType.GENERIC_TASK);
    }

    @Test
    public void unsuccessfulExecutionReturnsFailureInfo() {
        doThrow(new RuntimeException("test")).when(mImpressionListener).log(any(Impression.class));
        Impression impression = createImpression();
        mImpressionsLoggingTask = new ImpressionLoggingTask(mImpressionListener, impression);

        SplitTaskExecutionInfo result = mImpressionsLoggingTask.execute();

        assertEquals(result.getStatus(), SplitTaskExecutionStatus.ERROR);
        assertEquals(result.getTaskType(), SplitTaskType.GENERIC_TASK);
    }

    private static Impression createImpression() {
        return new Impression("key", "feature", "treatment", "on", 1402040204L, "label", 123123L, new HashMap<>());
    }
}
