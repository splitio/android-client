package io.split.sharedtest.rules;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import androidx.work.testing.SynchronousExecutor;
import androidx.work.testing.WorkManagerTestInitHelper;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Usage:
 *
 * @Rule public WorkManagerRule mWorkManagerRule = new WorkManagerRule();
 * <p>
 * Requires dependency in the test scope:
 * testImplementation "androidx.work:work-testing:<version>"
 */
public class WorkManagerRule implements TestRule {

    @NonNull
    @Override
    public Statement apply(@NonNull Statement base, @NonNull Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final Context context = ApplicationProvider.getApplicationContext();

                // Initialize WorkManager with a synchronous executor for deterministic behavior
                Configuration config = new Configuration.Builder()
                        .setMinimumLoggingLevel(Log.DEBUG)
                        .setExecutor(new SynchronousExecutor())
                        .build();

                WorkManagerTestInitHelper.initializeTestWorkManager(context, config);

                try {
                    base.evaluate();
                } finally {
                    // Best-effort cleanup between tests
                    try {
                        WorkManager wm = WorkManager.getInstance(context);
                        wm.cancelAllWork();
                        wm.pruneWork();
                    } catch (Throwable t) {
                        // Ignore cleanup errors to not mask test failures
                    }
                }
            }
        };
    }
}
