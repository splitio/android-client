package io.split.android.client.storage.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.split.android.client.impressions.Impression;

@Dao
public interface ImpressionDao {
    @Insert
    void insert(ImpressionEntity impression);

    @Query("SELECT id, test_name, body, timestamp, status FROM impressions " +
            "WHERE timestamp >= :timestamp " +
            "AND status = :status ORDER BY timestamp, test_name")
    List<ImpressionEntity> getBy(long timestamp, int status);

    @Query("UPDATE impressions SET status = :status " +
            " WHERE id IN (:ids)")
    void updateStatus(List<Long> ids, int status);

    @Query("DELETE FROM impressions WHERE id IN (:ids)")
    void delete(List<Long> ids);

    @Query("DELETE FROM impressions WHERE timestamp < :timestamp")
    void deleteOutdated(long timestamp);
}
