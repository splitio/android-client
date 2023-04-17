package io.split.android.client.storage.events;

import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.split.android.client.dtos.Event;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.EventDao;
import io.split.android.client.storage.db.EventEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;

public class SqLitePersistentEventsStorageTest {

    private static final long EXPIRATION_PERIOD = 1000L;

    @Mock
    private SplitRoomDatabase mDatabase;

    @Mock
    private EventDao mDao;

    @Mock
    private SplitCipher mSplitCipher;

    private SqLitePersistentEventsStorage mStorage;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mDatabase.eventDao()).thenReturn(mDao);
        mStorage = new SqLitePersistentEventsStorage(mDatabase, EXPIRATION_PERIOD, mSplitCipher);
    }

    @Test
    public void entityIsInsertedUsingDao() {
        when(mSplitCipher.encrypt(anyString())).thenReturn("encrypted_body");

        Event event = createTestEvent(0);
        EventEntity entity = mStorage.entityForModel(event);
        when(mSplitCipher.encrypt(anyString())).thenReturn(entity.getBody());

        mStorage.insert(entity);

        verify(mDao).insert(entity);
    }

    @Test
    public void entitiesAreInsertedUsingDao() {
        when(mSplitCipher.encrypt(anyString())).thenReturn("encrypted_body");
        List<Event> events = Arrays.asList(createTestEvent(0), createTestEvent(1));

        List<EventEntity> entities = new ArrayList<>();
        for (Event event : events) {
            entities.add(mStorage.entityForModel(event));
        }

        when(mSplitCipher.encrypt(anyString())).thenReturn(entities.get(0).getBody()).thenReturn(entities.get(1).getBody());

        mStorage.insert(entities);

        verify(mDao).insert(entities);
    }

    @Test
    public void deleteByStatusRemovesImpressionsUsingDaoWith100Limit() {
        mStorage.deleteByStatus(1, 1100);
        verify(mDao).deleteByStatus(1, 1100, 100);
    }

    @Test
    public void deleteOutDatedUsesDao() {
        mStorage.deleteOutdated(1000);
        verify(mDao).deleteOutdated(1000);
    }

    @Test
    public void deleteByIdUsesDao() {
        mStorage.deleteById(Collections.singletonList(1L));
        verify(mDao).delete(Collections.singletonList(1L));
    }

    @Test
    public void updateStatusUsesDao() {
        mStorage.updateStatus(Collections.singletonList(1L), 1);
        verify(mDao).updateStatus(Collections.singletonList(1L), 1);
    }

    @Test
    public void updateStatusWithMultipleIdsUsesDao() {
        mStorage.updateStatus(Arrays.asList(1L, 2L), 1);
        verify(mDao).updateStatus(Arrays.asList(1L, 2L), 1);
    }

    @Test
    public void entityToModelDecryptsBody() {
        when(mSplitCipher.decrypt("encrypted_body"))
                .thenReturn("{\"sizeInBytes\":0,\"eventTypeId\":\"test_event_0\"," +
                        "\"trafficTypeName\":\"custom\",\"key\":\"key1\",\"value\":0," +
                        "\"timestamp\":0,\"properties\":null}");

        EventEntity entity = new EventEntity();
        entity.setBody("encrypted_body");
        entity.setCreatedAt(1900000);
        entity.setStatus(1);

        mStorage.entityToModel(entity);

        verify(mSplitCipher).decrypt(entity.getBody());
    }

    @Test
    public void entityForModelEncryptsBody() {
        Event event = createTestEvent(0);
        when(mSplitCipher.encrypt(anyString())).thenReturn("encrypted_body");

        mStorage.entityForModel(event);

        verify(mSplitCipher).encrypt("{\"sizeInBytes\":0,\"eventTypeId\":\"test_event_0\"," +
                "\"trafficTypeName\":\"custom\",\"key\":\"key1\",\"value\":0,\"timestamp\":0," +
                "\"properties\":{}}");
    }

    @Test
    public void entityForModelReturnsNullWhenEncryptionResultIsNull() {
        when(mSplitCipher.encrypt(anyString())).thenReturn(null);

        EventEntity entity = mStorage.entityForModel(createTestEvent(0));

        assertNull(entity);
    }

    @Test
    public void runInTransactionCallsRunInTransactionOnDatabase() {
        when(mSplitCipher.encrypt(anyString())).thenReturn("encrypted_body");
        List<Event> events = Arrays.asList(createTestEvent(0), createTestEvent(1));

        List<EventEntity> entities = new ArrayList<>();
        for (Event event : events) {
            entities.add(mStorage.entityForModel(event));
        }

        when(mSplitCipher.encrypt(anyString())).thenReturn(entities.get(0).getBody()).thenReturn(entities.get(1).getBody());

        mStorage.runInTransaction(entities, 2, 1000L);

        verify(mDatabase).runInTransaction(any(SqLitePersistentEventsStorage.GetAndUpdate.class));
    }

    private Event createTestEvent(int index) {
        Event event = new Event();
        event.eventTypeId = "test_event_" + index;
        event.trafficTypeName = "custom";
        event.key = "key1";
        event.properties = Collections.emptyMap();

        return event;
    }
}
