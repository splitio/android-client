package io.split.android.client.storage.db;

import androidx.lifecycle.LiveData;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface SplitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<SplitEntity> splits);

    @Query("SELECT name, body, timestamp FROM splits")
    List<SplitEntity> getAll();
}
