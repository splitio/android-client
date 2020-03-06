package storage.migrator.mocks;

import androidx.core.util.Pair;

import java.util.List;

import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.migrator.SplitsMigratorHelper;

public class SplitsMigratorHelperMock implements SplitsMigratorHelper {
    private long mChangeNumber;
    private List<SplitEntity> mSplits;

    public void setSnapshot(long changeNumber, List<SplitEntity> splits) {
        mChangeNumber = changeNumber;
        mSplits = splits;
    }

    @Override
    public Pair<Long, List<SplitEntity>> loadLegacySplitsAsEntities() {
        return new Pair<>(mChangeNumber, mSplits);
    }
}
