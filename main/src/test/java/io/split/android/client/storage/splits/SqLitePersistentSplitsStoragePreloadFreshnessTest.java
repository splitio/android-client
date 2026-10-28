package io.split.android.client.storage.splits;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.database.Cursor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.split.android.client.dtos.Split;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.GeneralInfoDao;
import io.split.android.client.storage.db.RefusedWrites;
import io.split.android.client.storage.db.SplitDao;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitQueryDaoImpl;
import io.split.android.client.storage.db.SplitRoomDatabase;

/**
 * A real SplitQueryDaoImpl over an in-memory "splits" table: a flag written through one
 * storage must be what any storage reads afterwards, whatever the prefill captured.
 */
public class SqLitePersistentSplitsStoragePreloadFreshnessTest {

    private final Map<String, String> mTable = new TreeMap<>();
    private SplitRoomDatabase mDatabase;
    private SplitCipher mCipher;
    private SplitListTransformer<SplitEntity, Split> mToSplit;
    private SplitListTransformer<Split, SplitEntity> mToEntity;

    @Before
    public void setUp() {
        mDatabase = mock(SplitRoomDatabase.class);
        mCipher = mock(SplitCipher.class);
        when(mCipher.encrypt(any())).thenAnswer(i -> i.getArgument(0));
        when(mCipher.decrypt(any())).thenAnswer(i -> i.getArgument(0));

        SplitDao splitDao = mock(SplitDao.class);
        doAnswer(i -> {
            for (SplitEntity e : i.<List<SplitEntity>>getArgument(0)) {
                put(e.getName(), e.getBody());
            }
            return null;
        }).when(splitDao).insert(any(List.class));
        doAnswer(i -> {
            synchronized (mTable) {
                for (String n : i.<List<String>>getArgument(0)) {
                    mTable.remove(n);
                }
            }
            return null;
        }).when(splitDao).delete(any(List.class));
        when(mDatabase.splitDao()).thenReturn(splitDao);
        when(mDatabase.generalInfoDao()).thenReturn(mock(GeneralInfoDao.class));
        doAnswer(i -> {
            ((Runnable) i.getArgument(0)).run();
            return null;
        }).when(mDatabase).runInTransaction(any(Runnable.class));
        when(mDatabase.query(anyString(), any())).thenAnswer(i -> cursorOfTable());

        // body is the flag version
        mToSplit = new SplitListTransformer<SplitEntity, Split>() {
            @Override
            public List<Split> transform(List<SplitEntity> in) {
                List<Split> out = new ArrayList<>();
                for (SplitEntity e : in) {
                    out.add(toSplit(e));
                }
                return out;
            }

            @Override
            public List<Split> transform(Map<String, SplitEntity> in) {
                return transform(new ArrayList<>(in.values()));
            }
        };
        mToEntity = new SplitListTransformer<Split, SplitEntity>() {
            @Override
            public List<SplitEntity> transform(List<Split> in) {
                List<SplitEntity> out = new ArrayList<>();
                for (Split s : in) {
                    SplitEntity e = new SplitEntity();
                    e.setName(s.name);
                    e.setBody(String.valueOf(s.changeNumber));
                    out.add(e);
                }
                return out;
            }

            @Override
            public List<SplitEntity> transform(Map<String, Split> in) {
                return transform(new ArrayList<>(in.values()));
            }
        };

        // Created lazily: the real DAO starts a prefill thread that queries mDatabase, which must
        // not happen while this setUp is still stubbing it (Mockito binds stubbing to the last call).
        SplitQueryDaoImpl[] queryDao = new SplitQueryDaoImpl[1];
        doAnswer(i -> {
            if (queryDao[0] == null) {
                queryDao[0] = new SplitQueryDaoImpl(mDatabase);
            }
            return queryDao[0];
        }).when(mDatabase).getSplitQueryDao();
    }

    @After
    public void tearDown() {
        RefusedWrites.resetForTesting();
    }

    @Test
    public void snapshotOfAnotherStorageSeesFlagUpdatedAfterTheFirstSnapshot() {
        put("f", "1");
        SqLitePersistentSplitsStorage storageA = newStorage();
        assertEquals(1L, versionOf(storageA.getSnapshot(), "f"));

        storageA.update(change(split("f", 2)), Collections.emptyMap(), Collections.emptyMap());

        SqLitePersistentSplitsStorage storageB = newStorage();
        assertEquals(2L, versionOf(storageB.getSnapshot(), "f"));
    }

    @Test
    public void snapshotOfTheSameStorageSeesFlagUpdatedAfterTheFirstSnapshot() {
        put("f", "1");
        SqLitePersistentSplitsStorage storage = newStorage();
        assertEquals(1L, versionOf(storage.getSnapshot(), "f"));

        storage.update(change(split("f", 2)), Collections.emptyMap(), Collections.emptyMap());

        assertEquals(2L, versionOf(storage.getSnapshot(), "f"));
    }

    @Test
    public void snapshotSeesSingleSplitUpdateAndDelete() {
        put("f", "1");
        SqLitePersistentSplitsStorage storage = newStorage();
        storage.getSnapshot();

        storage.update(split("f", 3));
        assertEquals(3L, versionOf(storage.getSnapshot(), "f"));

        storage.delete(Collections.singletonList("f"));
        assertEquals(0, storage.getSnapshot().getSplits().size());
    }

    private SqLitePersistentSplitsStorage newStorage() {
        return new SqLitePersistentSplitsStorage(mDatabase, mToSplit, mToEntity, mCipher);
    }

    private static ProcessedSplitChange change(Split active) {
        return new ProcessedSplitChange(Collections.singletonList(active), Collections.emptyList(), 10, 1);
    }

    private static Split split(String name, long version) {
        Split s = new Split();
        s.name = name;
        s.changeNumber = version;
        return s;
    }

    private static Split toSplit(SplitEntity e) {
        return split(e.getName(), Long.parseLong(e.getBody()));
    }

    private static long versionOf(SplitsSnapshot snapshot, String name) {
        for (Split s : snapshot.getSplits()) {
            if (name.equals(s.name)) {
                return s.changeNumber;
            }
        }
        return -1;
    }

    private void put(String name, String body) {
        synchronized (mTable) {
            mTable.put(name, body);
        }
    }

    private Cursor cursorOfTable() {
        final List<String[]> rows = new ArrayList<>();
        synchronized (mTable) {
            for (Map.Entry<String, String> e : mTable.entrySet()) {
                rows.add(new String[]{e.getKey(), e.getValue()});
            }
        }
        Cursor cursor = mock(Cursor.class);
        when(cursor.getColumnIndex("name")).thenReturn(0);
        when(cursor.getColumnIndex("body")).thenReturn(1);
        final int[] pos = {-1};
        when(cursor.moveToNext()).thenAnswer(i -> ++pos[0] < rows.size());
        when(cursor.getString(0)).thenAnswer(i -> rows.get(pos[0])[0]);
        when(cursor.getString(1)).thenAnswer(i -> rows.get(pos[0])[1]);
        return cursor;
    }
}
