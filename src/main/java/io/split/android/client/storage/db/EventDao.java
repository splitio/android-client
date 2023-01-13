package io.split.android.client.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface EventDao {
    @Insert
    public void insert(EventEntity event);

    @Insert
    void insert(List<EventEntity> events);

    @Query("SELECT id, body, created_at, status FROM events " +
            "WHERE created_at >= :updateAt " +
            "AND status = :status ORDER BY created_at LIMIT :maxRows")
    List<EventEntity> getBy(long updateAt, int status, int maxRows);

    @Query("UPDATE events SET status = :status "  +
            " WHERE id IN (:ids)")
    void updateStatus(List<Long> ids, int status);

    @Query("DELETE FROM events WHERE id IN (:ids)")
    void delete(List<Long> ids);

    @Query("DELETE FROM events WHERE created_at < :updateAt")
    void deleteOutdated(long updateAt);

    @Query("DELETE FROM events WHERE  status = :status AND created_at < :maxTimestamp " +
            "AND EXISTS(SELECT 1 FROM events AS eve  WHERE eve.id = events.id LIMIT :maxRows)")
    int deleteByStatus(int status, long maxTimestamp, int maxRows);

}
