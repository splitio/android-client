package database;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;

public class GeneralInfoDaoTest {

    SplitRoomDatabase mRoomDb;
    Context mContext;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        mRoomDb = SplitRoomDatabase.getDatabase(mContext, "encripted_api_key");
        mRoomDb.clearAllTables();
    }

    @Test
    public void insertRetrieve() throws InterruptedException {
        long timestamp = System.currentTimeMillis();
        List<GeneralInfoEntity> data = generateLongInfo(1, 10);
        for(GeneralInfoEntity info : data) {
            mRoomDb.generalInfoDao().update(info);
        }

        data = generateStringInfo(11, 20);
        for(GeneralInfoEntity info : data) {
            mRoomDb.generalInfoDao().update(info);
        }

        for(int i = 1; i<=10; i++){
            GeneralInfoEntity infoEntity = mRoomDb.generalInfoDao().getByName("key_" + i);
            Assert.assertEquals("key_"+ i, infoEntity.getName());
            Assert.assertEquals(i, infoEntity.getLongValue());
        }

        for(int i = 11; i<=20; i++){
            GeneralInfoEntity infoEntity = mRoomDb.generalInfoDao().getByName("key_" + i);
            Assert.assertEquals("key_"+ i, infoEntity.getName());
            Assert.assertEquals("string_" + i, infoEntity.getStringValue());
        }
    }

    @Test
    public void insertUpdateRetrieve() throws InterruptedException {
        long timestamp = System.currentTimeMillis();
        List<GeneralInfoEntity> data = generateLongInfo(1, 3);
        for(GeneralInfoEntity info : data) {
            mRoomDb.generalInfoDao().update(info);
        }

        data = generateStringInfo(4, 6);
        for(GeneralInfoEntity info : data) {
            mRoomDb.generalInfoDao().update(info);
        }

        mRoomDb.generalInfoDao().update(new GeneralInfoEntity("key_2", 100));
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity("key_3", "value_100"));

        mRoomDb.generalInfoDao().update(new GeneralInfoEntity("key_5", 100));
        mRoomDb.generalInfoDao().update(new GeneralInfoEntity("key_6", "value_100"));

        List<GeneralInfoEntity> list = new ArrayList<>();
        for(int i = 1; i<=6; i++){
            list.add(mRoomDb.generalInfoDao().getByName("key_" + i));
        }

        Assert.assertEquals(1, list.get(0).getLongValue());
        Assert.assertEquals(100, list.get(1).getLongValue());
        Assert.assertEquals("value_100", list.get(2).getStringValue());
        Assert.assertEquals("string_4", list.get(3).getStringValue());
        Assert.assertEquals(100, list.get(4).getLongValue());
        Assert.assertEquals("value_100", list.get(5).getStringValue());
    }

    private List<GeneralInfoEntity> generateStringInfo(int valueFrom, int valueTo) {
        List<GeneralInfoEntity> list = new ArrayList<>();
        for(int i = valueFrom; i<=valueTo; i++){
            list.add(new GeneralInfoEntity("key_" + i, "string_" + i));
        }
        return list;
    }

    private List<GeneralInfoEntity> generateLongInfo(int valueFrom, int valueTo) {
        List<GeneralInfoEntity> list = new ArrayList<>();
        for(int i = valueFrom; i<=valueTo; i++){
            list.add(new GeneralInfoEntity("key_" + i, i));
        }
        return list;
    }
}
