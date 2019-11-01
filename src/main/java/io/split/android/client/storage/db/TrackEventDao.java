package io.split.android.client.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TrackEventDao {
    @Insert
    public void insert(TrackEventEntity event);

    @Query("SELECT id, body, timestamp, status FROM track_events " +
            "WHERE timestamp >= :timestamp " +
            "AND status = :status ORDER BY timestamp")
    List<TrackEventEntity> getBy(long timestamp, int status);

    @Query("UPDATE track_events SET status = :status "  +
            " WHERE id IN (:ids)")
    void updateStatus(List<Long> ids, int status);

    @Query("DELETE FROM track_events WHERE id IN (:ids)")
    void delete(List<Long> ids);

    @Query("DELETE FROM track_events WHERE timestamp < :timestamp")
    void deleteOutdated(long timestamp);

}
