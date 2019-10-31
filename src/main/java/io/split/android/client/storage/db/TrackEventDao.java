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
            "AND status = " + TrackEventEntity.STATUS_ACTIVE  + " ORDER BY timestamp")
    List<TrackEventEntity> getToSend(long timestamp);

    @Query("UPDATE track_events SET status = " + ImpressionEntity.STATUS_DELETED +
            " WHERE id IN (:ids)")
    void markAsDeleted(List<Integer> ids);

    @Query("DELETE FROM track_events WHERE status = " + ImpressionEntity.STATUS_DELETED)
    void delete();

    @Query("DELETE FROM track_events WHERE timestamp < :timestamp")
    void deleteOutdated(long timestamp);

}
