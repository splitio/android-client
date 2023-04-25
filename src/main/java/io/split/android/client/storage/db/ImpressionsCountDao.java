package io.split.android.client.storage.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ImpressionsCountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ImpressionsCountEntity count);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<ImpressionsCountEntity> counts);

    @Query("SELECT id, body, created_at, status FROM impressions_count " +
            "WHERE created_at >= :timestamp " +
            "AND status = :status ORDER BY created_at LIMIT :maxRows")
    List<ImpressionsCountEntity> getBy(long timestamp, int status, int maxRows);

    @Query("UPDATE impressions_count SET status = :status " +
            " WHERE id IN (:ids)")
    void updateStatus(List<Long> ids, int status);

    @Query("DELETE FROM impressions_count WHERE id IN (:ids)")
    void delete(List<Long> ids);

    @Query("DELETE FROM impressions_count WHERE created_at < :timestamp")
    void deleteOutdated(long timestamp);

    @Query("DELETE FROM impressions_count WHERE  status = :status AND created_at < :maxTimestamp " +
            "AND EXISTS(SELECT 1 FROM impressions_count AS imp  WHERE imp.id = impressions_count.id LIMIT :maxRows)")
    int deleteByStatus(int status, long maxTimestamp, int maxRows);

    @Query("SELECT id, body, created_at, status FROM impressions_count")
    List<ImpressionsCountEntity> getAll();
}
