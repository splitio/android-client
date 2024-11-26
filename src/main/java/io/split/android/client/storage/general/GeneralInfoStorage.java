package io.split.android.client.storage.general;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface GeneralInfoStorage {

    long getSplitsUpdateTimestamp();

    void setSplitsUpdateTimestamp(long timestamp);

    long getChangeNumber();

    void setChangeNumber(long changeNumber);

    @NonNull
    String getSplitsFilterQueryString();

    void setSplitsFilterQueryString(String queryString);

    String getDatabaseEncryptionMode();

    void setDatabaseEncryptionMode(String value);

    @Nullable
    String getFlagsSpec();

    void setFlagsSpec(String value);

    long getRolloutCacheLastClearTimestamp();

    void setRolloutCacheLastClearTimestamp(long timestamp);
}
