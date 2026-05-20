package tests.integration.rollout;

import static helper.IntegrationHelper.dummyApiKey;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.TimeUnit;

import helper.DatabaseHelper;
import io.split.android.client.SplitFilter;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.dtos.Status;
import io.split.android.client.service.splits.SplitChangeProcessor;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.splits.PersistentSplitsStorage;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.storage.splits.SplitsStorageImpl;
import io.split.android.client.storage.splits.SqLitePersistentSplitsStorage;

public class FreshInstallPrefetchPersistenceIntegrationTest {

    private static final long CHANGE_NUMBER = 1778482333302L;
    private static final int SPLIT_COUNT_OVER_ASYNC_THRESHOLD = 60;
    private static final String FIRST_FLAG_NAME = "fresh_install_flag_0";

    private SplitRoomDatabase mRoomDb;
    private PersistentSplitsStorage mPersistentStorage;
    private SplitChangeProcessor mSplitChangeProcessor;

    @Before
    public void setUp() {
        Context context = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = DatabaseHelper.getTestDatabase(context);
        mRoomDb.clearAllTables();

        SplitCipher cipher = SplitCipherFactory.create(dummyApiKey(), false);
        mPersistentStorage = new SqLitePersistentSplitsStorage(mRoomDb, cipher);
        mSplitChangeProcessor = new SplitChangeProcessor((Map<SplitFilter.Type, SplitFilter>) null, null);
    }

    @Test
    public void emptyDeltaDoesNotPersistChangeNumberBeforeFullSnapshotIsDurable() {
        PausedExecutorService pausedExecutor = new PausedExecutorService();
        SplitsStorage storage = new SplitsStorageImpl(mPersistentStorage);

        storage.update(
                mSplitChangeProcessor.process(SplitChange.create(-1, CHANGE_NUMBER, createSplits())),
                pausedExecutor);
        storage.update(
                mSplitChangeProcessor.process(SplitChange.create(CHANGE_NUMBER, CHANGE_NUMBER, new ArrayList<>())),
                pausedExecutor);

        SplitsStorage reloadedStorage = new SplitsStorageImpl(mPersistentStorage);
        reloadedStorage.loadLocal();

        assertEquals(CHANGE_NUMBER, reloadedStorage.getTill());
        assertEquals(SPLIT_COUNT_OVER_ASYNC_THRESHOLD, mRoomDb.splitDao().getAll().size());
        assertNotNull(reloadedStorage.get(FIRST_FLAG_NAME));
    }

    private static List<Split> createSplits() {
        List<Split> splits = new ArrayList<>();
        for (int i = 0; i < SPLIT_COUNT_OVER_ASYNC_THRESHOLD; i++) {
            Split split = new Split();
            split.name = "fresh_install_flag_" + i;
            split.status = Status.ACTIVE;
            split.changeNumber = CHANGE_NUMBER;
            split.trafficTypeName = "user";
            split.defaultTreatment = "on";
            split.killed = false;
            splits.add(split);
        }
        return splits;
    }

    private static class PausedExecutorService extends AbstractExecutorService {

        @Override
        public void shutdown() {
        }

        @Override
        public List<Runnable> shutdownNow() {
            return new ArrayList<>();
        }

        @Override
        public boolean isShutdown() {
            return false;
        }

        @Override
        public boolean isTerminated() {
            return false;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return false;
        }

        @Override
        public void execute(Runnable command) {
            // Keep submitted tasks queued forever to mimic a process kill before async persistence runs.
        }
    }
}
