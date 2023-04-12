package tests.storage;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import helper.DatabaseHelper;
import io.split.android.client.dtos.Split;
import io.split.android.client.service.executor.SplitTask;
import io.split.android.client.service.executor.SplitTaskExecutionInfo;
import io.split.android.client.service.executor.SplitTaskExecutionStatus;
import io.split.android.client.service.splits.LoadSplitsTask;
import io.split.android.client.storage.cipher.NoOpCipher;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.splits.SplitsStorage;
import io.split.android.client.storage.splits.SplitsStorageImpl;
import io.split.android.client.storage.splits.SqLitePersistentSplitsStorage;

public class LoadSplitTaskTest {

    final static Long INITIAL_CHANGE_NUMBER = 9999L;
    final String JSON_SPLIT_TEMPLATE = "{\"name\":\"%s\", \"changeNumber\": %d}";
    SplitRoomDatabase mRoomDb;
    Context mContext;
    SplitsStorage mSplitsStorage;

    @Before
    public void setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = DatabaseHelper.getTestDatabase(mContext);
        mRoomDb.clearAllTables();
        List<SplitEntity> entities = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            String splitName = "split-" + i;
            SplitEntity entity = new SplitEntity();
            entity.setName(splitName);
            entity.setBody(String.format(JSON_SPLIT_TEMPLATE, splitName, INITIAL_CHANGE_NUMBER - i));
            entities.add(entity);
        }
        mRoomDb.splitDao().insert(entities);
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, INITIAL_CHANGE_NUMBER));
        mSplitsStorage = new SplitsStorageImpl(new SqLitePersistentSplitsStorage(mRoomDb, new NoOpCipher()));
    }

    @Test
    public void execute() {

        SplitTask task = new LoadSplitsTask(mSplitsStorage);
        SplitTaskExecutionInfo result = task.execute();

        Split split0 = mSplitsStorage.get("split-0");
        Split split1 = mSplitsStorage.get("split-1");
        Split split2 = mSplitsStorage.get("split-2");
        Split split3 = mSplitsStorage.get("split-3");

        Assert.assertNotNull(split0);
        Assert.assertNotNull(split1);
        Assert.assertNotNull(split2);
        Assert.assertNotNull(split3);
        Assert.assertEquals(SplitTaskExecutionStatus.SUCCESS, result.getStatus());
    }
}
