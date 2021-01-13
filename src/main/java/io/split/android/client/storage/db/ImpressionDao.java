package io.split.android.client.storage.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

import io.split.android.client.impressions.Impression;

@Dao
public interface ImpressionDao {
    @Insert
    void insert(ImpressionEntity impression);

    @Insert
    void insert(List<ImpressionEntity> impressions);

    @Transaction
    @Query("SELECT id, test_name, body, created_at, status FROM impressions " +
            "WHERE created_at >= :timestamp " +
            "AND status = :status ORDER BY created_at LIMIT :maxRows")
    List<ImpressionEntity> getBy(long timestamp, int status, int maxRows);

    @Transaction
    @Query("UPDATE impressions SET status = :status " +
            " WHERE id IN (:ids)")
    void updateStatus(List<Long> ids, int status);

    @Transaction
    @Query("DELETE FROM impressions WHERE id IN (:ids)")
    void delete(List<Long> ids);

    @Transaction
    @Query("DELETE FROM impressions WHERE created_at < :timestamp")
    void deleteOutdated(long timestamp);

    @Query("DELETE FROM impressions WHERE  status = :status AND created_at < :maxTimestamp")
    void deleteByStatus(int status, long maxTimestamp);
}
