package io.split.android.client.storage.splits;

import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import com.google.gson.JsonSyntaxException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.dtos.Split;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.logger.Logger;

public class SplitEntityToSplitTransformer implements SplitListTransformer<SplitEntity, Split> {

    private final SplitCipher mSplitCipher;

    public SplitEntityToSplitTransformer(@NonNull SplitCipher splitCipher) {
        mSplitCipher = checkNotNull(splitCipher);
    }

    @Override
    public List<Split> transform(List<SplitEntity> entities) {
        return new ArrayList<>(); // no - op
    }

    @Override
    public List<Split> transform(Map<String, SplitEntity> allNamesAndBodies) {
        if (allNamesAndBodies == null) {
            return new ArrayList<>();
        }

        List<Split> splits = new ArrayList<>(allNamesAndBodies.size());
        for (Map.Entry<String, SplitEntity> entry : allNamesAndBodies.entrySet()) {
            if (entry == null || entry.getValue() == null) {
                continue;
            }
            try {
                String decryptedName = mSplitCipher.decrypt(entry.getKey());
                if (decryptedName == null) {
                    continue;
                }
                String decryptedBody = mSplitCipher.decrypt(entry.getValue().getBody());
                if (decryptedBody == null) {
                    continue;
                }

                splits.add(new Split(decryptedName, decryptedBody));
            } catch (JsonSyntaxException e) {
                Logger.e("Could not parse entity to split: " + entry.getKey());
            }
        }

        return splits;
    }
}
