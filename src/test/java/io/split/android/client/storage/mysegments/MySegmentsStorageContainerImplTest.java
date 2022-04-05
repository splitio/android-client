package io.split.android.client.storage.mysegments;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;

public class MySegmentsStorageContainerImplTest {

    @Mock
    PersistentMySegmentsStorage mPersistentMySegmentsStorage;
    private MySegmentsStorageContainerImpl mContainer;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        mContainer = new MySegmentsStorageContainerImpl(mPersistentMySegmentsStorage);
    }

    @Test
    public void getStorageForKeyReturnsNewStorageForEachKey() {
        String userKey = "user_key";
        String userKey2 = "user_key_2";

        MySegmentsStorage storageForKey = mContainer.getStorageForKey(userKey);
        MySegmentsStorage storageForKey2 = mContainer.getStorageForKey(userKey2);

        assertNotNull(storageForKey);
        assertNotNull(storageForKey2);
        assertNotEquals(storageForKey, storageForKey2);
    }

    @Test
    public void getStorageForKeyDoesNotCreateNewStorageForSameKey() {
        String userKey = "user_key";

        MySegmentsStorage storageForKey = mContainer.getStorageForKey(userKey);
        MySegmentsStorage storageForKeySecondRequest = mContainer.getStorageForKey(userKey);

        assertNotNull(storageForKey);
        assertEquals(storageForKey, storageForKeySecondRequest);
    }

    @Test
    public void getUniqueAmountReturnsUniqueSegmentCount() {
        String userKey = "user_key";
        String userKey2 = "user_key_2";

        MySegmentsStorage storageForKey = mContainer.getStorageForKey(userKey);
        MySegmentsStorage storageForKey2 = mContainer.getStorageForKey(userKey2);

        storageForKey.set(Arrays.asList("s1", "s2"));
        storageForKey2.set(Arrays.asList("s2", "s4", "s6"));

        long distinctAmount = mContainer.getUniqueAmount();

        assertEquals(4, distinctAmount);
    }
}
