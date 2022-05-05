package io.split.android.client.storage.db.impressions.unique;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface UniqueKeysDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UniqueKeyEntity uniqueKeyEntity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(List<UniqueKeyEntity> uniqueKeyEntityList);

    @Query("SELECT id, user_key, feature_list, created_at, status FROM unique_keys " +
            "WHERE created_at >= :fromTimestamp " +
            "AND status = :status ORDER BY created_at LIMIT :maxRows")
    List<UniqueKeyEntity> getBy(long fromTimestamp, int status, int maxRows);

    @Query("UPDATE unique_keys SET status = :status " +
            " WHERE user_key IN (:userKeys)")
    void updateStatus(List<String> userKeys, int status);

    @Query("DELETE FROM unique_keys WHERE user_key IN (:userKeys)")
    void delete(List<String> userKeys);

    @Query("DELETE FROM unique_keys WHERE created_at < :beforeTimestamp")
    void deleteOutdated(long beforeTimestamp);

    @Query("DELETE FROM unique_keys WHERE status = :status AND created_at < :maxTimestamp " +
            "AND EXISTS(SELECT 1 FROM unique_keys AS imp WHERE imp.user_key = unique_keys.user_key LIMIT :maxRows)")
    int deleteByStatus(int status, long maxTimestamp, int maxRows);

    @Query("SELECT id, user_key, feature_list, created_at, status FROM unique_keys")
    List<UniqueKeyEntity> getAll();

    @Query("DELETE FROM unique_keys WHERE id IN (:ids)")
    void deleteById(List<Long> ids);
}
