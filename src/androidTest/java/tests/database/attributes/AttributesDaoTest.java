package tests.database.attributes;

import org.junit.Assert;
import org.junit.Test;

import io.split.android.client.storage.db.attributes.AttributesEntity;
import tests.database.GenericDaoTest;

public class AttributesDaoTest extends GenericDaoTest {

    @Test
    public void whenCallingUpdateValuesAreInsertedCorrectly() {
        mRoomDb.attributesDao().update(new AttributesEntity("key1", "attributes1", 100L));
        mRoomDb.attributesDao().update(new AttributesEntity("key2", "attributes2", 200L));
        mRoomDb.attributesDao().update(new AttributesEntity("key3", "attributes3", 300L));

        AttributesEntity entity1 = mRoomDb.attributesDao().getByUserKey("key1");
        AttributesEntity entity2 = mRoomDb.attributesDao().getByUserKey("key2");
        AttributesEntity entity3 = mRoomDb.attributesDao().getByUserKey("key3");

        Assert.assertEquals("key1", entity1.getUserKey());
        Assert.assertEquals("key2", entity2.getUserKey());
        Assert.assertEquals("key3", entity3.getUserKey());

        Assert.assertEquals("attributes1", entity1.getAttributes());
        Assert.assertEquals("attributes2", entity2.getAttributes());
        Assert.assertEquals("attributes3", entity3.getAttributes());

        Assert.assertEquals(100L, entity1.getUpdatedAt());
        Assert.assertEquals(200L, entity2.getUpdatedAt());
        Assert.assertEquals(300L, entity3.getUpdatedAt());
    }

    @Test
    public void nullAttributesValueIsInsertedCorrectly() {
        mRoomDb.attributesDao().update(new AttributesEntity("key1", null, 100L));

        Assert.assertNull(mRoomDb.attributesDao().getByUserKey("key1").getAttributes());
    }

    @Test
    public void deleteAllRemovesEntriesFromAttributesTableAssociatedWithUserKey() {
        mRoomDb.attributesDao().update(new AttributesEntity("key1", "attributes1", 100L));
        mRoomDb.attributesDao().update(new AttributesEntity("key3", "attributes3", 300L));

        mRoomDb.attributesDao().deleteAll("key1");

        AttributesEntity attributesForKey1 = mRoomDb.attributesDao().getByUserKey("key1");
        AttributesEntity attributesForKey3 = mRoomDb.attributesDao().getByUserKey("key3");

        Assert.assertNull(attributesForKey1);
        Assert.assertNotNull(attributesForKey3);
    }
}
