package io.split.android.client.service;

import androidx.arch.core.executor.TaskExecutor;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static java.lang.Thread.sleep;

public class SplitTaskExecutorTest {

    SplitTaskExecutor mTaskExecutor;

    @Before
    public void setup() {
        mTaskExecutor = new SplitTaskExecutorImpl();
    }

    @Test
    public void simpleSubmit() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        TestTask task = new TestTask(latch);

        mTaskExecutor.submit(task);
        latch.await(5, TimeUnit.SECONDS);

        Assert.assertTrue(task.taskHasBeenCalled);
    }

    @Test
    public void multipleSubmit() throws InterruptedException {
        final int taskCount = 10;
        CountDownLatch latch = new CountDownLatch(taskCount);
        List<TestTask> taskList = new ArrayList<>();
        for (int i = 0; i < taskCount; i++) {
            taskList.add(new TestTask(latch));
        }

        for (int i = 0; i < taskCount; i++) {
            mTaskExecutor.submit(taskList.get(i));
        }

        latch.await(10, TimeUnit.SECONDS);
        for (int i = 0; i < taskCount; i++) {
            Assert.assertTrue(taskList.get(i).taskHasBeenCalled);
        }
    }

    @Test
    public void submitOnPause() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        TestTask task = new TestTask(latch);

        mTaskExecutor.pause();
        mTaskExecutor.submit(task);
        latch.await(5, TimeUnit.SECONDS);
        boolean executedOnPause = task.taskHasBeenCalled;
        mTaskExecutor.resume();
        latch.await(5, TimeUnit.SECONDS);
        boolean executedOnResume = task.taskHasBeenCalled;

        Assert.assertFalse(executedOnPause);
        Assert.assertTrue(executedOnResume);
    }

    @Test
    public void submitOnStop() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        TestTask task = new TestTask(latch);
        TestTask taskOnStop = new TestTask(latch);

        mTaskExecutor.submit(task);
        latch.await(5, TimeUnit.SECONDS);
        boolean executedBeforeStop = task.taskHasBeenCalled;

        mTaskExecutor.stop();
        mTaskExecutor.submit(taskOnStop);
        latch.await(5, TimeUnit.SECONDS);
        boolean executedOnStop = taskOnStop.taskHasBeenCalled;

        Assert.assertTrue(executedBeforeStop);
        Assert.assertFalse(executedOnStop);
    }

    @Test
    public void simpleScheduled() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(4);
        TestTask task = new TestTask(latch);

        mTaskExecutor.schedule(task, 0L, 1);
        latch.await(10, TimeUnit.SECONDS);

        Assert.assertTrue(task.taskHasBeenCalled);
        Assert.assertEquals(4, task.callCount);
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
            mTaskExecutor.schedule(taskList.get(i), 0L, i + 1);
        }

        latch.await(10, TimeUnit.SECONDS);
        for (int i = 0; i < taskCount; i++) {
            Assert.assertTrue(taskList.get(i).taskHasBeenCalled);
        }
        Assert.assertTrue(4 < taskList.get(0).callCount);
    }

    @Test
    public void scheduledOnPause() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        TestTask task = new TestTask(latch);

        mTaskExecutor.pause();
        mTaskExecutor.schedule(task, 0L, 1);
        latch.await(5, TimeUnit.SECONDS);
        boolean executedOnPause = task.taskHasBeenCalled;
        mTaskExecutor.resume();
        latch.await(5, TimeUnit.SECONDS);
        boolean executedOnResume = task.taskHasBeenCalled;

        Assert.assertFalse(executedOnPause);
        Assert.assertTrue(executedOnResume);
    }

    @Test
    public void pauseScheduled() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(3);
        CountDownLatch latch1 = new CountDownLatch(1);
        TestTask task = new TestTask(latch);
        TestTask task1 = new TestTask(latch);

        mTaskExecutor.schedule(task, 0L, 1);
        mTaskExecutor.schedule(task1, 6L, 20);
        latch.await(5, TimeUnit.SECONDS);
        int countBeforePause = task.callCount;
        int count1BeforePause = task1.callCount;

        mTaskExecutor.pause();

        // Using sleep to make a pause in this thread
        // then resumes task executor
        // and wait for task 1 latch
        sleep(3);
        int countAfterPause = task.callCount;
        int count1AfterPause = task1.callCount;

        mTaskExecutor.resume();
        latch1.await(5, TimeUnit.SECONDS);

        int countAfterResume = task.callCount;
        int count1AfterResume = task1.callCount;

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

        mTaskExecutor.schedule(task, 0L, 1);
        latch.await(5, TimeUnit.SECONDS);
        boolean executedBeforeStop = task.taskHasBeenCalled;

        mTaskExecutor.stop();
        mTaskExecutor.schedule(taskOnStop, 0L, 1);
        latch.await(5, TimeUnit.SECONDS);
        boolean executedOnStop = taskOnStop.taskHasBeenCalled;

        Assert.assertTrue(executedBeforeStop);
        Assert.assertFalse(executedOnStop);
    }

    @Test
    public void exceptionInScheduled() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(4);
        TestTask task = new TestTask(latch);
        task.shouldThrowException = true;

        mTaskExecutor.schedule(task, 0L, 1);
        latch.await(10, TimeUnit.SECONDS);

        Assert.assertTrue(task.taskHasBeenCalled);
        Assert.assertEquals(4, task.callCount);
    }

    @After
    public void tearDown() {
    }

    class TestTask implements SplitTask {
        CountDownLatch latch;

        TestTask(CountDownLatch latch) {
            this.latch = latch;
        }

        public boolean shouldThrowException = false;
        public int callCount = 0;
        public boolean taskHasBeenCalled = false;

        @Override
        public void execute() {
            callCount++;
            taskHasBeenCalled = true;
            latch.countDown();
            if(shouldThrowException) {
                throw new IllegalStateException();
            }
        }
    }
}
