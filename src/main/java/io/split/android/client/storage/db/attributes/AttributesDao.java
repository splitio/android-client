package io.split.android.client.storage.db.attributes;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AttributesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void update(AttributesEntity attributesEntity);

    @Query("UPDATE attributes SET user_key = :userKey, attributes = :attributes WHERE user_key = :formerUserKey")
    void update(String formerUserKey, String userKey, String attributes);

    @Query("SELECT user_key, attributes, updated_at FROM attributes WHERE user_key = :userKey")
    AttributesEntity getByUserKey(String userKey);

    @Query("DELETE FROM attributes WHERE user_key = :userKey")
    void deleteAll(String userKey);

    @Query("SELECT user_key, attributes, updated_at FROM attributes")
    List<AttributesEntity> getAll();
}
