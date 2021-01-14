package io.split.android.client.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface SplitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<SplitEntity> splits);

    @Transaction
    @Query("DELETE FROM splits WHERE name IN (:names)")
    void delete(List<String> names);

    @Transaction
    @Query("SELECT name, body, updated_at FROM splits")
    List<SplitEntity> getAll();

    @Transaction
    @Query("DELETE FROM splits")
    void deleteAll();
}
