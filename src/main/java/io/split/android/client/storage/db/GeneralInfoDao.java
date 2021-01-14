package io.split.android.client.storage.db;

import androidx.lifecycle.LiveData;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface GeneralInfoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void update(GeneralInfoEntity info);

    @Transaction
    @Query("SELECT name, stringValue, longValue, updated_at FROM general_info WHERE name = :name")
    GeneralInfoEntity getByName(String name);
}
