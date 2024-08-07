package io.split.android.client.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static java.lang.Thread.sleep;

import android.os.Handler;

import androidx.annotation.NonNull;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskBatchItem;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionListener;
import io.split.android.client.service.executor.SplitTaskExecutorImpl;
import io.split.android.client.service.executor.SplitTaskType;

public class SplitTaskExecutorTest {

    private SplitTaskExecutorImpl mTaskExecutor;

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
        mTaskExecutor = new SplitTaskExecutorImpl();
    }

    @Test
    public void simpleSubmit() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        TestTask task = new TestTask(latch);

        CountDownLatch listenerLatch = new CountDownLatch(1);
        TestListener listener = new TestListener(listenerLatch);

        mTaskExecutor.submit(task, listener);
        latch.await(15, TimeUnit.SECONDS);
        listenerLatch.await(15, TimeUnit.SECONDS);

        Assert.assertTrue(task.taskHasBeenCalled);
        Assert.assertTrue(listener.taskExecutedCalled);
    }

    @Test
    public void multipleSubmit() throws InterruptedException {
        final int taskCount = 10;
        CountDownLatch latch = new CountDownLatch(taskCount);

        CountDownLatch listenerLatch = new CountDownLatch(taskCount);
        TestListener listener = new TestListener(listenerLatch);

        List<TestTask> taskList = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            taskList.add(new TestTask(latch));
        }

        for (int i = 0; i < taskCount; i++) {
            mTaskExecutor.submit(taskList.get(i), listener);
        }

        latch.await(10, TimeUnit.SECONDS);

        listenerLatch.await(15, TimeUnit.SECONDS);
        for (int i = 0; i < taskCount; i++) {
            Assert.assertTrue(taskList.get(i).taskHasBeenCalled);
        }
        Assert.assertTrue(listener.taskExecutedCalled);
    }

    @Test
    public void submitOnPause() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        TestTask task = new TestTask(latch);

        mTaskExecutor.pause();
        mTaskExecutor.submit(task, mock(SplitTaskExecutionListener.class));
        latch.await(5, TimeUnit.SECONDS);
        boolean executedOnPause = task.taskHasBeenCalled;
        mTaskExecutor.resume();
        latch.await(5, TimeUnit.SECONDS);
        boolean executedOnResume = task.taskHasBeenCalled;

        assertFalse(executedOnPause);
        Assert.assertTrue(executedOnResume);
    }

    @Test
    public void submitOnStop() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        TestTask task = new TestTask(latch);
        TestTask taskOnStop = new TestTask(latch);

        mTaskExecutor.submit(task, mock(SplitTaskExecutionListener.class));
        latch.await(5, TimeUnit.SECONDS);
        boolean executedBeforeStop = task.taskHasBeenCalled;

        mTaskExecutor.stop();
        mTaskExecutor.submit(taskOnStop, mock(SplitTaskExecutionListener.class));
        latch.await(5, TimeUnit.SECONDS);
        boolean executedOnStop = taskOnStop.taskHasBeenCalled;

        Assert.assertTrue(executedBeforeStop);
        assertFalse(executedOnStop);
    }

    @Test
    public void simpleScheduled() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(4);
        TestTask task = new TestTask(latch);

        mTaskExecutor.schedule(
                task, 0L, 1, mock(SplitTaskExecutionListener.class));
        latch.await(10, TimeUnit.SECONDS);

        Assert.assertTrue(task.taskHasBeenCalled);
        Assert.assertEquals(4, task.callCount.get());
    }

    @Test
    public void multipleScheduled() throws InterruptedException {
        final int taskCount = 4;
        CountDownLatch latch = new CountDownLatch(taskCount * 3);
        List<TestTask> taskList = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            taskList.add(new TestTask(latch));
        }

        for (int i = 0; i < taskCount; i++) {
            mTaskExecutor.schedule(
                    taskList.get(i), 0L, i + 1,
                    mock(SplitTaskExecutionListener.class));
        }

        latch.await(10, TimeUnit.SECONDS);
        for (int i = 0; i < taskCount; i++) {
            Assert.assertTrue(taskList.get(i).taskHasBeenCalled);
        }
        Assert.assertTrue(4 < taskList.get(0).callCount.get());
    }

    @Test
    public void scheduledOnPause() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        TestTask task = new TestTask(latch);

        mTaskExecutor.pause();
        mTaskExecutor.schedule(
                task, 0L, 1, mock(SplitTaskExecutionListener.class));
        latch.await(5, TimeUnit.SECONDS);
        boolean executedOnPause = task.taskHasBeenCalled;
        mTaskExecutor.resume();
        latch.await(5, TimeUnit.SECONDS);
        boolean executedOnResume = task.taskHasBeenCalled;

        assertFalse(executedOnPause);
        Assert.assertTrue(executedOnResume);
    }

    @Test
    public void pauseScheduled() throws InterruptedException {

//      This test schedules 2 task, one to be executed every one sec without delay
//      and the other with an initial delay of 6 secs every 20 secs
//      Call count is taken before pause, then the executor is resumed
//      and call count is taken again
//      At the end call count is checked for both tasks

        CountDownLatch latch = new CountDownLatch(3);
        CountDownLatch latch1 = new CountDownLatch(1);
        TestTask task = new TestTask(latch);
        TestTask task1 = new TestTask(latch);

        mTaskExecutor.schedule(
                task, 0L, 1, mock(SplitTaskExecutionListener.class));
        mTaskExecutor.schedule(
                task1, 6L, 20, mock(SplitTaskExecutionListener.class));
        latch.await(5, TimeUnit.SECONDS);
        int countBeforePause = task.callCount.get();
        int count1BeforePause = task1.callCount.get();

        mTaskExecutor.pause();

        // Using sleep to make a pause in this thread
        // then resumes task executor
        // and wait for task 1 latch
        sleep(3);
        int countAfterPause = task.callCount.get();
        int count1AfterPause = task1.callCount.get();

        mTaskExecutor.resume();
        latch1.await(5, TimeUnit.SECONDS);

        int count1AfterResume = task1.callCount.get();

        Assert.assertEquals(3, countBeforePause);
        Assert.assertEquals(0, count1BeforePause);
        Assert.assertEquals(3, countAfterPause);
        Assert.assertEquals(0, count1AfterPause);

        Assert.assertEquals(1, count1AfterResume);
    }

    @Test
    public void scheduleOnStop() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        TestTask task = new TestTask(latch);
        TestTask taskOnStop = new TestTask(latch);

        mTaskExecutor.schedule(
                task, 0L, 1, mock(SplitTaskExecutionListener.class));
        latch.await(5, TimeUnit.SECONDS);
        boolean executedBeforeStop = task.taskHasBeenCalled;

        mTaskExecutor.stop();
        mTaskExecutor.schedule(
                taskOnStop, 0L, 1,
                mock(SplitTaskExecutionListener.class));
        latch.await(5, TimeUnit.SECONDS);
        boolean executedOnStop = taskOnStop.taskHasBeenCalled;

        Assert.assertTrue(executedBeforeStop);
        assertFalse(executedOnStop);
    }

    @Test
    public void exceptionInScheduled() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(4);
        TestTask task = new TestTask(latch);
        task.shouldThrowException = true;

        mTaskExecutor.schedule(
                task, 0L, 1, mock(SplitTaskExecutionListener.class));
        latch.await(10, TimeUnit.SECONDS);

        Assert.assertTrue(task.taskHasBeenCalled);
        Assert.assertEquals(4, task.callCount.get());
    }

    @Test
    public void stopScheduledTask() {
        CountDownLatch latch = new CountDownLatch(1);
        TestTask task = new TestTask(latch);

        String taskId = mTaskExecutor.schedule(task, 10L, 10L, mock(SplitTaskExecutionListener.class));
        mTaskExecutor.stopTask(taskId);

        assertFalse(task.taskHasBeenCalled);
    }

    @Test
    public void stopStartedTask() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(4);
        CountDownLatch timerLatch = new CountDownLatch(1);
        CountDownLatch listenerLatch = new CountDownLatch(1);
        TestTask task = new TestTask(latch);

        TestListener testListener = new TestListener(listenerLatch);
        String taskId = mTaskExecutor.schedule(task, 0L, 1L, testListener);
        timerLatch.await(2L, TimeUnit.SECONDS);
        mTaskExecutor.stopTask(taskId);

        assertTrue(task.taskHasBeenCalled);
        assertTrue(testListener.taskExecutedCalled);
        assertEquals(2, task.callCount.get());
    }

    @Test
    public void scheduleDelayedTask() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        CountDownLatch listenerLatch = new CountDownLatch(1);
        TestTask task = new TestTask(latch);
        TestListener testListener = new TestListener(listenerLatch);

        mTaskExecutor.schedule(task, 0L, testListener);

        listenerLatch.await(2L, TimeUnit.SECONDS);
        assertTrue(task.taskHasBeenCalled);
        assertTrue(testListener.taskExecutedCalled);
        assertEquals(1, task.callCount.get());
    }

    @Test
    public void schedulingAfterShutdown() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(4);
        CountDownLatch timerLatch = new CountDownLatch(2);
        CountDownLatch listenerLatch = new CountDownLatch(1);
        TestTask task = new TestTask(latch);

        TestListener testListener = new TestListener(listenerLatch);
        mTaskExecutor.schedule(task, 0L, 1L, testListener);
        timerLatch.await(1500L, TimeUnit.MILLISECONDS);
        mTaskExecutor.stop();
        timerLatch.await(4L, TimeUnit.SECONDS);

        TestTask newTask = new TestTask(new CountDownLatch(1));
        mTaskExecutor.schedule(newTask, 0L, 1L, mock(SplitTaskExecutionListener.class));

        assertTrue(task.taskHasBeenCalled);
        assertTrue(testListener.taskExecutedCalled);
        assertEquals(2, task.callCount.get());
        assertFalse(newTask.taskHasBeenCalled);
    }

    @After
    public void tearDown() {
        mTaskExecutor.stop();
    }

    static class TestTask implements SplitTask {
        CountDownLatch latch;
        SplitTaskType type;

        TestTask(CountDownLatch latch) {
            this.latch = latch;
            this.type = SplitTaskType.GENERIC_TASK;
        }

        TestTask(CountDownLatch latch, SplitTaskType taskType) {
            this.latch = latch;
            this.type = taskType;
        }

        public boolean shouldThrowException = false;
        public AtomicInteger callCount = new AtomicInteger(0);
        public boolean taskHasBeenCalled = false;

        @NonNull
        @Override
        public SplitTaskExecutionInfo execute() {
            callCount.incrementAndGet();
            taskHasBeenCalled = true;
            latch.countDown();
            if (shouldThrowException) {
                throw new IllegalStateException();
            }
            return SplitTaskExecutionInfo.success(type);
        }
    }

    static class TestListener implements SplitTaskExecutionListener {
        CountDownLatch mLatch;
        boolean taskExecutedCalled = false;

        public TestListener(CountDownLatch latch) {
            mLatch = latch;
        }

        @Override
        public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
            taskExecutedCalled = true;
            mLatch.countDown();
        }
    }

    static class CompletionTracker {
        private final LinkedBlockingQueue<Integer> _done;

        public CompletionTracker(int length) {
            _done = new LinkedBlockingQueue<>(length);
        }

        void track(int i) {
            _done.offer(i);
        }

        List<Integer> getAll() {
            return new ArrayList<>(_done);
        }
    }

    static class SerialListener implements SplitTaskExecutionListener {
        CompletionTracker _tracker;
        private int mTaskNumber = -1;
        private final CountDownLatch mLatch;

        public SerialListener(int taskNumber, CompletionTracker tracker, CountDownLatch latch) {
            mTaskNumber = taskNumber;
            _tracker = tracker;
            mLatch = latch;
        }

        @Override
        public void taskExecuted(@NonNull SplitTaskExecutionInfo taskInfo) {
            _tracker.track(mTaskNumber);
            mLatch.countDown();
        }
    }

    @Test
    public void executeSerially() throws InterruptedException {
        final int taskCount = 4;
        CompletionTracker tracker = new CompletionTracker(4);
        List<SerialListener> listeners = new ArrayList<>();
        // Enqueing 4 task to run serially
        // Listener is identified by an integer
        CountDownLatch latch = new CountDownLatch(taskCount * 2);
        List<SplitTaskBatchItem> taskList = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            listeners.add(new SerialListener(i, tracker, latch));
            taskList.add(new SplitTaskBatchItem(new TestTask(latch), listeners.get(i)));
        }

        // Executing tasks serially
        mTaskExecutor.executeSerially(taskList);

        // Awaiting to coundown latches in tasks
        boolean result = latch.await(40, TimeUnit.SECONDS);
        Assert.assertTrue(result);


        List<Integer> tracked = tracker.getAll();
        for (int i = 0; i < taskCount; i++) {
            Assert.assertEquals(i, tracked.get(i).intValue());
        }
    }

    @Test
    public void testSubmitOnMainThread() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        SplitTask mockTask = mock(SplitTask.class);
        Handler mockHandler = mock(Handler.class);
        when(mockHandler.post(captor.capture())).thenAnswer(invocation -> {
            captor.getValue().run();
            return true;
        });

        mTaskExecutor.submitOnMainThread(mockHandler, mockTask);

        verify(mockTask).execute();
    }


    @Test
    public void testSubmitOnMainThreadWithException() {
        ArgumentCaptor<Runnable> captor = ArgumentCaptor.forClass(Runnable.class);
        SplitTask mockTask = mock(SplitTask.class);
        when(mockTask.execute()).thenThrow(new RuntimeException());
        Handler mockHandler = mock(Handler.class);
        when(mockHandler.post(captor.capture())).thenAnswer(invocation -> {
            captor.getValue().run();
            return true;
        });

        mTaskExecutor.submitOnMainThread(mockHandler, mockTask);

        verify(mockTask).execute();
    }
}
