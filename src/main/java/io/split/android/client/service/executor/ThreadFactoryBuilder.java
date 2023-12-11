package io.split.android.client.service.executor;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Objects.requireNonNull;

import androidx.annotation.Nullable;

import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;

public final class ThreadFactoryBuilder {

    @Nullable
    private String nameFormat = null;
    @Nullable
    private Boolean daemon = null;
    @Nullable
    private Integer priority = null;
    @Nullable
    private Thread.UncaughtExceptionHandler uncaughtExceptionHandler = null;
    @Nullable
    private ThreadFactory backingThreadFactory = null;

    /**
     * Creates a new {@link ThreadFactory} builder.
     */
    public ThreadFactoryBuilder() {
    }

    /**
     * Sets the naming format to use when naming threads ({@link Thread#setName}) which are created
     * with this ThreadFactory.
     *
     * @param nameFormat a {@link String#format(String, Object...)}-compatible format String, to which
     *                   a unique integer (0, 1, etc.) will be supplied as the single parameter. This integer will
     *                   be unique to the built instance of the ThreadFactory and will be assigned sequentially. For
     *                   example, {@code "rpc-pool-%d"} will generate thread names like {@code "rpc-pool-0"}, {@code
     *                   "rpc-pool-1"}, {@code "rpc-pool-2"}, etc.
     * @return this for the builder pattern
     */
    public ThreadFactoryBuilder setNameFormat(String nameFormat) {
        String unused = format(nameFormat, 0); // fail fast if the format is bad or null
        this.nameFormat = nameFormat;
        return this;
    }

    /**
     * Sets daemon or not for new threads created with this ThreadFactory.
     *
     * @param daemon whether or not new Threads created with this ThreadFactory will be daemon threads
     * @return this for the builder pattern
     */
    public ThreadFactoryBuilder setDaemon(boolean daemon) {
        this.daemon = daemon;
        return this;
    }

    /**
     * Sets the priority for new threads created with this ThreadFactory.
     *
     * <p><b>Warning:</b> relying on the thread scheduler is <a
     * href="http://errorprone.info/bugpattern/ThreadPriorityCheck">discouraged</a>.
     *
     * @param priority the priority for new Threads created with this ThreadFactory
     * @return this for the builder pattern
     */
    public ThreadFactoryBuilder setPriority(int priority) {
        // Thread#setPriority() already checks for validity. These error messages
        // are nicer though and will fail-fast.
        checkArgument(priority >= Thread.MIN_PRIORITY);
        checkArgument(priority <= Thread.MAX_PRIORITY);
        this.priority = priority;
        return this;
    }

    /**
     * Sets the {@link Thread.UncaughtExceptionHandler} for new threads created with this ThreadFactory.
     *
     * @param uncaughtExceptionHandler the uncaught exception handler for new Threads created with
     *                                 this ThreadFactory
     * @return this for the builder pattern
     */
    public ThreadFactoryBuilder setUncaughtExceptionHandler(
            Thread.UncaughtExceptionHandler uncaughtExceptionHandler) {
        this.uncaughtExceptionHandler = checkNotNull(uncaughtExceptionHandler);
        return this;
    }

    /**
     * Sets the backing {@link ThreadFactory} for new threads created with this ThreadFactory. Threads
     * will be created by invoking #newThread(Runnable) on this backing {@link ThreadFactory}.
     *
     * @param backingThreadFactory the backing {@link ThreadFactory} which will be delegated to during
     *                             thread creation.
     * @return this for the builder pattern
     */
    public ThreadFactoryBuilder setThreadFactory(ThreadFactory backingThreadFactory) {
        this.backingThreadFactory = checkNotNull(backingThreadFactory);
        return this;
    }

    /**
     * Returns a new thread factory using the options supplied during the building process. After
     * building, it is still possible to change the options used to build the ThreadFactory and/or
     * build again. State is not shared amongst built instances.
     *
     * @return the fully constructed {@link ThreadFactory}
     */
    public ThreadFactory build() {
        return doBuild(this);
    }

    // Split out so that the anonymous ThreadFactory can't contain a reference back to the builder.
    // At least, I assume that's why. TODO(cpovirk): Check, and maybe add a test for this.
    private static ThreadFactory doBuild(ThreadFactoryBuilder builder) {
        String nameFormat = builder.nameFormat;
        Boolean daemon = builder.daemon;
        Integer priority = builder.priority;
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = builder.uncaughtExceptionHandler;
        ThreadFactory backingThreadFactory =
                (builder.backingThreadFactory != null)
                        ? builder.backingThreadFactory
                        : Executors.defaultThreadFactory();
        AtomicLong count = (nameFormat != null) ? new AtomicLong(0) : null;
        return new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = backingThreadFactory.newThread(runnable);
                // TODO(b/139735208): Figure out what to do when the factory returns null.
                requireNonNull(thread);
                if (nameFormat != null) {
                    // requireNonNull is safe because we create `count` if (and only if) we have a nameFormat.
                    thread.setName(format(nameFormat, requireNonNull(count).getAndIncrement()));
                }
                if (daemon != null) {
                    thread.setDaemon(daemon);
                }
                if (priority != null) {
                    thread.setPriority(priority);
                }
                if (uncaughtExceptionHandler != null) {
                    thread.setUncaughtExceptionHandler(uncaughtExceptionHandler);
                }
                return thread;
            }
        };
    }

    private static String format(String format, Object... args) {
        return String.format(Locale.ROOT, format, args);
    }
}
