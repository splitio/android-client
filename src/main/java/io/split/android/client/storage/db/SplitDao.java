package io.split.android.client.storage.db;

import androidx.room.ColumnInfo;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.MapInfo;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;
import java.util.Map;

@Dao
public interface SplitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<SplitEntity> splits);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(SplitEntity split);

    @Query("UPDATE splits SET name = :name, body = :body, trafficType = :trafficType, sets = :sets WHERE name = :formerName")
    void update(String formerName, String name, String body, String trafficType, String sets);

    @Query("DELETE FROM splits WHERE name IN (:names)")
    void delete(List<String> names);

    @Query("SELECT name, body, trafficType, sets, updated_at FROM splits")
    List<SplitEntity> getAll();

    @Query("DELETE FROM splits")
    void deleteAll();
}
