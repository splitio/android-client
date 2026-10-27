package io.split.android.client.perf;

import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeTrue;

import android.content.Context;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.Test;
import org.junit.runner.RunWith;

import helper.TestableSplitConfigBuilder;
import io.split.android.client.PerfCacheSeeder;
import io.split.android.client.PerfSeedSpec;
import io.split.android.client.PerfSeedSpec.Spec;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import okhttp3.mockwebserver.MockWebServer;

/**
 * Gated by the {@code perf} instrumentation argument. {@code seed} writes the cache for the
 * {@code enc}, {@code flags} and {@code sets} arguments, conditions it with one launch, and stores it
 * as the template that later runs restore. {@code launch} restores the template and measures one
 * launch, reporting it as a {@code SPLITPERF_RESULT} log line and as the {@code splitPerf} status
 * entry. One test per process, so run each as its own {@code am instrument} call.
 */
@RunWith(AndroidJUnit4.class)
public class ReadyTimingTest {

    private static final String TAG = "SplitPerf";

    private static final String STATUS_KEY = "splitPerf";
    private static final long CONDITIONING_WAIT_MS = 10_000;
    private static final long POLL_MS = 100;

    private static boolean sUsedInThisProcess;

    @Test
    public void seed() throws Exception {
        Bundle args = InstrumentationRegistry.getArguments();
        assumeTrue("perf runs only: -e perf 1", PerfGate.isEnabled(args.getString(PerfGate.ARG)));
        assertFalse("one seed per am instrument call", sUsedInThisProcess);
        sUsedInThisProcess = true;

        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        Spec spec = Spec.parse(args.getString("enc"), args.getString("flags"), args.getString("sets"));
        String dbName = PerfSeedSpec.databaseName(new TestableSplitConfigBuilder().build());

        long startMs = System.currentTimeMillis();
        long ciphertextBytes = PerfCacheSeeder.writeRawSeed(context, dbName, spec);
        conditionWithOneLaunch(context, dbName, spec);
        PerfCacheSeeder.storeTemplate(context, dbName, spec, startMs, ciphertextBytes);

        Log.i(TAG, "seeded " + spec.id() + " " + PerfCacheSeeder.describeRows(context, dbName)
                + " ciphertextBytes=" + ciphertextBytes);
    }

    @Test
    public void launch() throws Exception {
        Bundle args = InstrumentationRegistry.getArguments();
        assumeTrue("perf runs only: -e perf 1", PerfGate.isEnabled(args.getString(PerfGate.ARG)));
        assertFalse("one launch per am instrument call", sUsedInThisProcess);
        sUsedInThisProcess = true;

        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        Spec spec = Spec.parse(args.getString("enc"), args.getString("flags"), args.getString("sets"));
        PerfScenarioArgs scenario = PerfScenarioArgs.parse(args.getString("latencyMs"),
                args.getString("run"), args.getString("git"));
        String dbName = PerfSeedSpec.databaseName(new TestableSplitConfigBuilder().build());

        // Files only, before any SDK class touches the database.
        PerfCacheSeeder.restoreTemplate(context, dbName, spec);

        StubDispatcher stub = new StubDispatcher(scenario.latencyMs);
        MockWebServer server = new MockWebServer();
        server.setDispatcher(stub);
        server.start();
        PerfLaunch.Result result = null;
        try {
            result = PerfLaunch.run(context, PerfLaunch.config(server, spec), spec.flags, stub);
            // After both events, so the key store probe is outside the measured window.
            JsonObject device = JsonParser.parseString(PerfDevice.describe(PerfSeedSpec.SDK_KEY).toString())
                    .getAsJsonObject();
            JsonObject manifest = JsonParser.parseString(PerfCacheSeeder.manifest(context, spec).toString())
                    .getAsJsonObject();
            String json = PerfReport.toJson(scenario.git, scenario.run, spec.id(), spec.flags, spec.rbs,
                    spec.encrypted, spec.flagSets, scenario.latencyMs, device, Process.myPid(),
                    result.buildNs, result.readyFromCacheNs, result.readyNs, result.readyFromCachePath,
                    result.firstFlagsSince, result.splitNames, manifest);
            Log.i(TAG, "SPLITPERF_RESULT " + json);
            Bundle status = new Bundle();
            status.putString(STATUS_KEY, json);
            InstrumentationRegistry.getInstrumentation().sendStatus(2, status);
        } finally {
            if (result != null) {
                result.factory.destroy();
            }
            server.shutdown();
        }
    }

    /**
     * Runs the SDK once against the seed so the database is left as a launch leaves it, then closes
     * this process's handle so the file on disk is complete before it is copied.
     */
    private static void conditionWithOneLaunch(Context context, String dbName, Spec spec) throws Exception {
        long seededStamp = splitsUpdateTimestamp(context, dbName);
        StubDispatcher stub = new StubDispatcher(0);
        MockWebServer server = new MockWebServer();
        server.setDispatcher(stub);
        server.start();
        try {
            SplitClientConfig config = PerfLaunch.config(server, spec);
            PerfLaunch.Result result = PerfLaunch.run(context, config, spec.flags, stub);
            try {
                waitForTimestampChange(context, dbName, seededStamp);
            } finally {
                result.factory.destroy();
            }
            SplitRoomDatabase.getDatabase(context, dbName).close();
        } finally {
            server.shutdown();
        }
    }

    private static void waitForTimestampChange(Context context, String dbName, long seededStamp)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + CONDITIONING_WAIT_MS;
        while (splitsUpdateTimestamp(context, dbName) == seededStamp) {
            if (System.currentTimeMillis() > deadline) {
                Log.w(TAG, GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP + " did not change within "
                        + CONDITIONING_WAIT_MS + " ms of the conditioning launch");
                return;
            }
            Thread.sleep(POLL_MS);
        }
    }

    private static long splitsUpdateTimestamp(Context context, String dbName) {
        SQLiteDatabase db = SQLiteDatabase.openDatabase(context.getDatabasePath(dbName).getPath(), null,
                SQLiteDatabase.OPEN_READONLY);
        try {
            return DatabaseUtils.longForQuery(db, "SELECT longValue FROM general_info WHERE name = ?",
                    new String[]{GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP});
        } finally {
            db.close();
        }
    }
}
