package io.split.android.client.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface GeneralInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void update(GeneralInfoEntity info);

    @Query("SELECT name, stringValue, longValue, updated_at FROM general_info WHERE name = :name")
    GeneralInfoEntity getByName(String name);
}
