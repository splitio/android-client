package io.split.android.client.service.executor.parallel;

import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public class SplitParallelTaskExecutorImplTest {

    private SplitParallelTaskExecutor<String> executor;

    @Before
    public void setUp() {
        executor = new SplitParallelTaskExecutorFactoryImpl().create(String.class);
    }

    @Test
    public void executeReturnsTheListOfResults() {
        List<String> expectedResult = Arrays.asList("yes", "no");
        List<SplitDeferredTaskItem<String>> listOfTasks = new ArrayList<>();

        listOfTasks.add(
                new SplitDeferredTaskItem<>(() -> {
                    Thread.sleep(80);

                    return "yes";
                })
        );

        listOfTasks.add(
                new SplitDeferredTaskItem<>(() -> {
                    Thread.sleep(100);

                    return "no";
                })
        );

        List<String> results = executor.execute(listOfTasks);

        assertTrue(results.containsAll(expectedResult));
    }

    @Test
    public void tasksStartExecutingSimultaneously() {
        List<SplitDeferredTaskItem<String>> listOfTasks = new ArrayList<>();
        AtomicLong yesStartTime = new AtomicLong();
        AtomicLong noStartTime = new AtomicLong();

        listOfTasks.add(
                new SplitDeferredTaskItem<>(() -> {
                    yesStartTime.set(System.currentTimeMillis());
                    Thread.sleep(80);

                    return "yes";
                })
        );

        listOfTasks.add(
                new SplitDeferredTaskItem<>(() -> {
                    noStartTime.set(System.currentTimeMillis());
                    Thread.sleep(100);

                    return "no";
                })
        );

        executor.execute(listOfTasks);

        assertTrue(Math.abs(yesStartTime.get() - noStartTime.get()) < 2);
    }

    @Test
    public void resultIsReturnedWhenComputationExceeds5Seconds() {
        List<SplitDeferredTaskItem<String>> splitDeferredTaskItems = Collections.singletonList(
                new SplitDeferredTaskItem<>(() -> {
                    Thread.sleep(6000);

                    return "no";
                })
        );

        long startTime = System.currentTimeMillis();
        executor.execute(splitDeferredTaskItems);

        assertTrue(System.currentTimeMillis() - startTime < 6000);
    }
}
