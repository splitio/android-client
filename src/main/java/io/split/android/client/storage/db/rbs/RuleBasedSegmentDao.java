package io.split.android.client.storage.db.rbs;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface RuleBasedSegmentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<RuleBasedSegmentEntity> entities);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RuleBasedSegmentEntity entity);

    @Query("UPDATE rule_based_segments SET name = :name, body = :body WHERE name = :formerName")
    void update(String formerName, String name, String body);

    @Query("DELETE FROM rule_based_segments WHERE name IN (:names)")
    void delete(List<String> names);

    @Query("SELECT name, body, updated_at FROM rule_based_segments")
    List<RuleBasedSegmentEntity> getAll();

    @Query("DELETE FROM rule_based_segments")
    void deleteAll();
}
