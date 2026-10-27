package io.split.android.client.storage.splits;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Split;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.db.SplitEntity;

public class SplitEntityToSplitTransformer implements SplitListTransformer<SplitEntity, Split> {

    private final SplitCipher mSplitCipher;

    public SplitEntityToSplitTransformer(@NonNull SplitCipher splitCipher) {
        mSplitCipher = checkNotNull(splitCipher);
    }

    @Override
    @Deprecated
    public List<Split> transform(List<SplitEntity> entities) {
        return new ArrayList<>(); // no - op
    }

    @Override
    public List<Split> transform(Map<String, SplitEntity> allNamesAndBodies) {
        if (allNamesAndBodies == null) {
            return new ArrayList<>();
        }

        List<String> stored = new ArrayList<>(allNamesAndBodies.size() * 2);
        for (Map.Entry<String, SplitEntity> entry : allNamesAndBodies.entrySet()) {
            if (entry == null || entry.getValue() == null) {
                continue;
            }
            stored.add(entry.getKey());
            stored.add(entry.getValue().getBody());
        }

        List<String> plain = SplitCipherFactory.decryptAll(mSplitCipher, stored);
        List<Split> splits = new ArrayList<>(plain.size() / 2);
        for (int i = 0; i < plain.size(); i += 2) {
            String name = plain.get(i);
            String body = plain.get(i + 1);
            if (name != null && body != null) {
                splits.add(new Split(name, body));
            }
        }

        return splits;
    }
}
