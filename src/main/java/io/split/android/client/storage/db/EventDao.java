package io.split.android.client.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EventDao {
    @Insert
    public void insert(EventEntity event);

    @Query("SELECT id, body, updated_at, status FROM events " +
            "WHERE updated_at >= :updateAt " +
            "AND status = :status ORDER BY updated_at LIMIT :maxRows")
    List<EventEntity> getBy(long updateAt, int status, int maxRows);

    @Query("UPDATE events SET status = :status "  +
            " WHERE id IN (:ids)")
    void updateStatus(List<Long> ids, int status);

    @Query("DELETE FROM events WHERE id IN (:ids)")
    void delete(List<Long> ids);

    @Query("DELETE FROM events WHERE updated_at < :updateAt")
    void deleteOutdated(long updateAt);

}
