package io.split.android.client.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EventDao {
    @Insert
    public void insert(EventEntity event);

    @Query("SELECT id, body, timestamp, status FROM events " +
            "WHERE timestamp >= :timestamp " +
            "AND status = :status ORDER BY timestamp")
    List<EventEntity> getBy(long timestamp, int status);

    @Query("UPDATE events SET status = :status "  +
            " WHERE id IN (:ids)")
    void updateStatus(List<Long> ids, int status);

    @Query("DELETE FROM events WHERE id IN (:ids)")
    void delete(List<Long> ids);

    @Query("DELETE FROM events WHERE timestamp < :timestamp")
    void deleteOutdated(long timestamp);

}
