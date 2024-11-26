package io.split.android.client.storage.general;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.storage.db.GeneralInfoDao;
import io.split.android.client.storage.db.GeneralInfoEntity;

public class GeneralInfoStorageImplTest {

    private GeneralInfoDao mGeneralInfoDao;
    private GeneralInfoStorageImpl mGeneralInfoStorage;

    @Before
    public void setUp() {
        mGeneralInfoDao = mock(GeneralInfoDao.class);
        mGeneralInfoStorage = new GeneralInfoStorageImpl(mGeneralInfoDao);
    }

    @Test
    public void setSplitsUpdateTimestampSetsValueOnDao() {
        mGeneralInfoStorage.setSplitsUpdateTimestamp(123L);

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals("splitsUpdateTimestamp") && entity.getLongValue() == 123L));
    }

    @Test
    public void setSplitsUpdateTimestampGetsValueFromDao() {
        when(mGeneralInfoDao.getByName("splitsUpdateTimestamp")).thenReturn(new GeneralInfoEntity("splitsUpdateTimestamp", 123L));
        long splitsUpdateTimestamp = mGeneralInfoStorage.getSplitsUpdateTimestamp();

        assertEquals(123L, splitsUpdateTimestamp);
        verify(mGeneralInfoDao).getByName("splitsUpdateTimestamp");
    }

    @Test
    public void setChangeNumberSetsValueOnDao() {
        mGeneralInfoStorage.setChangeNumber(123L);

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals("splitChangesChangeNumber") && entity.getLongValue() == 123L));
    }

    @Test
    public void setChangeNumberGetsValueFromDao() {
        when(mGeneralInfoDao.getByName("splitChangesChangeNumber")).thenReturn(new GeneralInfoEntity("splitChangesChangeNumber", 123L));
        long changeNumber = mGeneralInfoStorage.getChangeNumber();

        assertEquals(123L, changeNumber);
        verify(mGeneralInfoDao).getByName("splitChangesChangeNumber");
    }

    @Test
    public void getSplitsFilterQueryStringGetsValueFromDao() {
        when(mGeneralInfoDao.getByName("splitsFilterQueryString")).thenReturn(new GeneralInfoEntity("splitsFilterQueryString", "queryString"));
        String splitsFilterQueryString = mGeneralInfoStorage.getSplitsFilterQueryString();

        assertEquals("queryString", splitsFilterQueryString);
        verify(mGeneralInfoDao).getByName("splitsFilterQueryString");
    }

    @Test
    public void setSplitsFilterQueryStringSetsValueOnDao() {
        mGeneralInfoStorage.setSplitsFilterQueryString("queryString");

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals("splitsFilterQueryString") && entity.getStringValue().equals("queryString")));
    }

    @Test
    public void getDatabaseEncryptionModeGetsValueFromDao() {
        when(mGeneralInfoDao.getByName("databaseEncryptionMode")).thenReturn(new GeneralInfoEntity("databaseEncryptionMode", "value"));
        String databaseEncryptionMode = mGeneralInfoStorage.getDatabaseEncryptionMode();

        assertEquals("value", databaseEncryptionMode);
        verify(mGeneralInfoDao).getByName("databaseEncryptionMode");
    }

    @Test
    public void setDatabaseEncryptionModeSetsValueOnDao() {
        mGeneralInfoStorage.setDatabaseEncryptionMode("value");

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals("databaseEncryptionMode") && entity.getStringValue().equals("value")));
    }

    @Test
    public void getFlagsSpecGetsValueFromDao() {
        when(mGeneralInfoDao.getByName("flagsSpec")).thenReturn(new GeneralInfoEntity("flagsSpec", "value"));
        String flagsSpec = mGeneralInfoStorage.getFlagsSpec();

        assertEquals("value", flagsSpec);
        verify(mGeneralInfoDao).getByName("flagsSpec");
    }

    @Test
    public void setFlagsSpecSetsValueOnDao() {
        mGeneralInfoStorage.setFlagsSpec("value");

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals("flagsSpec") && entity.getStringValue().equals("value")));
    }

    @Test
    public void getRolloutCacheLastClearTimestampGetsValueFromDao() {
        when(mGeneralInfoDao.getByName("rolloutCacheLastClearTimestamp")).thenReturn(new GeneralInfoEntity("rolloutCacheLastClearTimestamp", 123L));
        long rolloutCacheLastClearTimestamp = mGeneralInfoStorage.getRolloutCacheLastClearTimestamp();

        assertEquals(123L, rolloutCacheLastClearTimestamp);
        verify(mGeneralInfoDao).getByName("rolloutCacheLastClearTimestamp");
    }

    @Test
    public void setRolloutCacheLastClearTimestampSetsValueOnDao() {
        mGeneralInfoStorage.setRolloutCacheLastClearTimestamp(123L);

        verify(mGeneralInfoDao).update(argThat(entity -> entity.getName().equals("rolloutCacheLastClearTimestamp") && entity.getLongValue() == 123L));
    }

    @Test
    public void getChangeNumberReturnsMinusOneIfEntityIsNull() {
        when(mGeneralInfoDao.getByName("splitChangesChangeNumber")).thenReturn(null);
        long changeNumber = mGeneralInfoStorage.getChangeNumber();

        assertEquals(-1L, changeNumber);
    }

    @Test
    public void getSplitsUpdateTimestampReturnsZeroIfEntityIsNull() {
        when(mGeneralInfoDao.getByName("splitsUpdateTimestamp")).thenReturn(null);
        long timestamp = mGeneralInfoStorage.getSplitsUpdateTimestamp();

        assertEquals(0L, timestamp);
    }

    @Test
    public void getSplitsFilterQueryStringReturnsEmptyStringIfEntityIsNull() {
        when(mGeneralInfoDao.getByName("splitsFilterQueryString")).thenReturn(null);
        String queryString = mGeneralInfoStorage.getSplitsFilterQueryString();

        assertEquals("", queryString);
    }

    @Test
    public void getDatabaseEncryptionModeReturnsEmptyStringIfEntityIsNull() {
        when(mGeneralInfoDao.getByName("databaseEncryptionMode")).thenReturn(null);
        String value = mGeneralInfoStorage.getDatabaseEncryptionMode();

        assertEquals("", value);
    }

    @Test
    public void getFlagsSpecReturnsEmptyStringIfEntityIsNull() {
        when(mGeneralInfoDao.getByName("flagsSpec")).thenReturn(null);
        String value = mGeneralInfoStorage.getFlagsSpec();

        assertEquals("", value);
    }

    @Test
    public void getRolloutCacheLastClearTimestampReturnsZeroIfEntityIsNull() {
        when(mGeneralInfoDao.getByName("rolloutCacheLastClearTimestamp")).thenReturn(null);
        long timestamp = mGeneralInfoStorage.getRolloutCacheLastClearTimestamp();

        assertEquals(0L, timestamp);
    }
}
