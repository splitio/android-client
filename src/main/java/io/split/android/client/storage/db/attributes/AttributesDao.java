package io.split.android.client.storage.db.attributes;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface AttributesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void update(AttributesEntity attributesEntity);

    @Query("SELECT user_key, attributes, updated_at FROM attributes WHERE user_key = :userKey")
    AttributesEntity getByUserKey(String userKey);

    @Query("DELETE FROM attributes WHERE user_key = :userKey")
    void deleteAll(String userKey);
}
