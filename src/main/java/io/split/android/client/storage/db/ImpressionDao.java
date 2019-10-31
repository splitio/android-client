package io.split.android.client.storage.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import io.split.android.client.impressions.Impression;

@Dao
public interface ImpressionDao {
    @Update
    void update(ImpressionEntity impression);

    @Query("SELECT id, test_name, body, timestamp, status FROM impressions " +
            "WHERE timestamp >= :timestamp " +
            "AND status = " + ImpressionEntity.STATUS_ACTIVE  + " ORDER BY timestamp, test_name")
    List<ImpressionEntity> getToSend(long timestamp);

    @Query("UPDATE impressions SET status = " + ImpressionEntity.STATUS_DELETED +
            " WHERE id IN (:ids)")
    void markAsDeleted(List<Integer> ids);

    @Query("DELETE FROM impressions WHERE status = " + ImpressionEntity.STATUS_DELETED)
    void delete();

    @Query("DELETE FROM impressions WHERE timestamp < :timestamp")
    void deleteOutdated(long timestamp);
}
