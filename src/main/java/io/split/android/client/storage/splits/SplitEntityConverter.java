package io.split.android.client.storage.splits;

import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.storage.db.SplitEntity;

public interface SplitEntityConverter {

    List<Split> getFromEntityList(List<SplitEntity> entities);

    List<SplitEntity> getFromSplitList(List<Split> splits);
}
