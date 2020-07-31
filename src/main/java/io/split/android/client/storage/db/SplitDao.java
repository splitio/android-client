package io.split.android.client.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SplitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<SplitEntity> splits);

    @Query("DELETE FROM splits WHERE name IN (:names)")
    void delete(List<String> names);

    @Query("SELECT name, body, updated_at FROM splits")
    List<SplitEntity> getAll();

    @Query("DELETE FROM splits")
    void deleteAll();
}
