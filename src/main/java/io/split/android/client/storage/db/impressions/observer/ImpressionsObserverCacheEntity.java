package io.split.android.client.storage.db.impressions.observer;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "impressions_observer_cache")
public class ImpressionsObserverCacheEntity {

    @PrimaryKey
    @ColumnInfo(name = "hash")
    private long hash;
    @ColumnInfo(name = "time")
    private long time;
    @ColumnInfo(name = "created_at")
    private long createdAt;

    public ImpressionsObserverCacheEntity() {
        // Room constructor
    }

    @Ignore
    public ImpressionsObserverCacheEntity(long hash, long time, long createdAt) {
        this.hash = hash;
        this.time = time;
        this.createdAt = createdAt;
    }

    public long getHash() {
        return hash;
    }

    public void setHash(long hash) {
        this.hash = hash;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
