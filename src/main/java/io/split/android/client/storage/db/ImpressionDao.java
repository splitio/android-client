package io.split.android.client.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ImpressionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ImpressionEntity impression);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<ImpressionEntity> impressions);

    @Query("SELECT id, test_name, body, created_at, status FROM impressions " +
            "WHERE created_at >= :timestamp " +
            "AND status = :status ORDER BY created_at LIMIT :maxRows")
    List<ImpressionEntity> getBy(long timestamp, int status, int maxRows);

    @Query("UPDATE impressions SET status = :status " +
            " WHERE id IN (:ids)")
    void updateStatus(List<Long> ids, int status);

    @Query("DELETE FROM impressions WHERE id IN (:ids)")
    void delete(List<Long> ids);

    @Query("DELETE FROM impressions WHERE created_at < :timestamp")
    void deleteOutdated(long timestamp);

    @Query("DELETE FROM impressions WHERE  status = :status AND created_at < :maxTimestamp " +
            "AND EXISTS(SELECT 1 FROM impressions AS imp  WHERE imp.id = impressions.id LIMIT :maxRows)")
    int deleteByStatus(int status, long maxTimestamp, int maxRows);

    @Query("SELECT id, test_name, body, created_at, status FROM impressions")
    List<ImpressionEntity> getAll();
}
