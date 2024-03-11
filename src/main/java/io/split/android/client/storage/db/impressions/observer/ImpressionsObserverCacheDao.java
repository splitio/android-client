package io.split.android.client.storage.db.impressions.observer;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ImpressionsObserverCacheDao {

    @Query("INSERT OR REPLACE INTO impressions_observer_cache (hash, time, created_at) VALUES (:hash, :time, :createdAt)")
    void insert(Long hash, Long time, Long createdAt);

    @Query("SELECT hash, time, created_at FROM impressions_observer_cache ORDER BY created_at ASC LIMIT :limit")
    List<ImpressionsObserverEntity> getAll(int limit);

    @Query("DELETE FROM impressions_observer_cache WHERE hash = :hash")
    void delete(Long hash);

    @Query("DELETE FROM impressions_observer_cache WHERE created_at < :timestamp")
    void deleteOldest(long timestamp);
}
