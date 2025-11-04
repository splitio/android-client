package io.split.android.client.service.executor;

import static org.junit.Assert.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.junit.Before;
import org.junit.Test;

public class ThreadFactoryBuilderTest {
    private final Runnable monitoredRunnable = new Runnable() {
        @Override public void run() {
            completed = true;
        }
    };
    private static final Thread.UncaughtExceptionHandler UNCAUGHT_EXCEPTION_HANDLER =
            (t, e) -> {
                // No-op
            };
    private ThreadFactoryBuilder builder;
    private volatile boolean completed = false;

    @Before
    public void setUp() {
        builder = new ThreadFactoryBuilder();
    }

    @Test
    public void testThreadFactoryBuilder_defaults() throws InterruptedException {
        ThreadFactory threadFactory = builder.build();
        Thread thread = threadFactory.newThread(monitoredRunnable);
        checkThreadPoolName(thread, 1);
        Thread defaultThread =
                Executors.defaultThreadFactory().newThread(monitoredRunnable);
        assertEquals(defaultThread.isDaemon(), thread.isDaemon());
        assertEquals(defaultThread.getPriority(), thread.getPriority());
        assertSame(defaultThread.getThreadGroup(), thread.getThreadGroup());
        assertSame(defaultThread.getUncaughtExceptionHandler(),
                thread.getUncaughtExceptionHandler());
        assertFalse(completed);
        thread.start();
        thread.join();
        assertTrue(completed);
        // Creating a new thread from the same ThreadFactory will have the same
        // pool ID but a thread ID of 2.
        Thread thread2 = threadFactory.newThread(monitoredRunnable);
        checkThreadPoolName(thread2, 2);
        assertEquals(
                thread.getName().substring(0, thread.getName().lastIndexOf('-')),
                thread2.getName().substring(0, thread.getName().lastIndexOf('-')));
        // Building again should give us a different pool ID.
        ThreadFactory threadFactory2 = builder.build();
        Thread thread3 = threadFactory2.newThread(monitoredRunnable);
        checkThreadPoolName(thread3, 1);
        assertNotEquals(thread2.getName().substring(0, thread.getName().lastIndexOf('-')), thread3.getName().substring(0, thread.getName().lastIndexOf('-')));
    }
    private static void checkThreadPoolName(Thread thread, int threadId) {
        assertTrue(thread.getName().matches("^pool-\\d+-thread-" + threadId + "$"));
    }

    @Test
    public void testNameFormatWithPercentS_custom() {
        String format = "super-duper-thread-%s";
        ThreadFactory factory = builder.setNameFormat(format).build();
        for (int i = 0; i < 11; i++) {
            assertEquals(String.format(format, i),
                    factory.newThread(monitoredRunnable).getName());
        }
    }

    @Test
    public void testNameFormatWithPercentD_custom() {
        String format = "super-duper-thread-%d";
        ThreadFactory factory = builder.setNameFormat(format).build();
        for (int i = 0; i < 11; i++) {
            assertEquals(String.format(format, i),
                    factory.newThread(monitoredRunnable).getName());
        }
    }

    @Test
    public void testDaemon_false() {
        ThreadFactory factory = builder.setDaemon(false).build();
        Thread thread = factory.newThread(monitoredRunnable);
        assertFalse(thread.isDaemon());
    }

    @Test
    public void testDaemon_true() {
        ThreadFactory factory = builder.setDaemon(true).build();
        Thread thread = factory.newThread(monitoredRunnable);
        assertTrue(thread.isDaemon());
    }

    @Test
    public void testPriority_custom() {
        for (int i = Thread.MIN_PRIORITY; i <= Thread.MAX_PRIORITY; i++) {
            ThreadFactory factory = builder.setPriority(i).build();
            Thread thread = factory.newThread(monitoredRunnable);
            assertEquals(i, thread.getPriority());
        }
    }

    @Test
    public void testPriority_tooLow() {
        try {
            builder.setPriority(Thread.MIN_PRIORITY - 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testPriority_tooHigh() {
        try {
            builder.setPriority(Thread.MAX_PRIORITY + 1);
            fail();
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testUncaughtExceptionHandler_custom() {
        assertEquals(UNCAUGHT_EXCEPTION_HANDLER,
                builder.setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER).build()
                        .newThread(monitoredRunnable).getUncaughtExceptionHandler());
    }

    @Test
    public void testBuildMutateBuild() {
        ThreadFactory factory1 = builder.setPriority(1).build();
        assertEquals(1, factory1.newThread(monitoredRunnable).getPriority());
        ThreadFactory factory2 = builder.setPriority(2).build();
        assertEquals(1, factory1.newThread(monitoredRunnable).getPriority());
        assertEquals(2, factory2.newThread(monitoredRunnable).getPriority());
    }

    @Test
    public void testBuildTwice() {
        builder.build();  // this is allowed
        builder.build();  // this is *also* allowed
    }

    @Test
    public void testBuildMutate() {
        ThreadFactory factory1 = builder.setPriority(1).build();
        assertEquals(1, factory1.newThread(monitoredRunnable).getPriority());
        builder.setPriority(2);  // change the state of the builder
        assertEquals(1, factory1.newThread(monitoredRunnable).getPriority());
    }

    @Test
    public void testThreadFactory() throws InterruptedException {
        final String THREAD_NAME = "ludicrous speed";
        final int THREAD_PRIORITY = 1;
        final boolean THREAD_DAEMON = false;
        ThreadFactory backingThreadFactory = new ThreadFactory() {
            @Override public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setName(THREAD_NAME);
                thread.setPriority(THREAD_PRIORITY);
                thread.setDaemon(THREAD_DAEMON);
                thread.setUncaughtExceptionHandler(UNCAUGHT_EXCEPTION_HANDLER);
                return thread;
            }
        };
        Thread thread = builder.setThreadFactory(backingThreadFactory).build()
                .newThread(monitoredRunnable);
        assertEquals(THREAD_NAME, thread.getName());
        assertEquals(THREAD_PRIORITY, thread.getPriority());
        assertEquals(THREAD_DAEMON, thread.isDaemon());
        assertSame(UNCAUGHT_EXCEPTION_HANDLER,
                thread.getUncaughtExceptionHandler());
        assertSame(Thread.State.NEW, thread.getState());
        assertFalse(completed);
        thread.start();
        thread.join();
        assertTrue(completed);
    }
}
