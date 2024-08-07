package io.split.android.client.storage.mysegments;

import static io.split.android.client.storage.mysegments.SegmentChangeDTO.createEmpty;
import static io.split.android.client.utils.Utils.checkNotNull;

import androidx.annotation.NonNull;

import com.google.gson.JsonParseException;

import java.util.Arrays;
import java.util.List;

import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.db.SegmentDao;
import io.split.android.client.storage.db.SegmentEntity;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Utils;
import io.split.android.client.utils.logger.Logger;

public class SqLitePersistentMySegmentsStorage<T extends SegmentEntity> implements PersistentMySegmentsStorage {

    private final SegmentDao<T> mDao;
    private final SplitCipher mSplitCipher;
    private final SegmentEntity.Creator<T> mCreator;

    public SqLitePersistentMySegmentsStorage(@NonNull SplitCipher splitCipher, SegmentDao<T> mySegmentDao, SegmentEntity.Creator<T> creator) {
        mDao = mySegmentDao;
        mSplitCipher = checkNotNull(splitCipher);
        mCreator = checkNotNull(creator);
    }

    @Override
    public void set(String userKey, @NonNull List<String> mySegments, long till) {
        if (mySegments == null) {
            return;
        }

        String encryptedUserKey = mSplitCipher.encrypt(userKey);
        String dto = Json.toJson(new SegmentChangeDTO(mySegments, till));
        String encryptedDto = mSplitCipher.encrypt(dto);
        if (encryptedUserKey == null || encryptedDto == null) {
            Logger.e("Error encrypting my segments");
            return;
        }
        T entity = mCreator.createEntity(encryptedUserKey, encryptedDto, System.currentTimeMillis() / 1000);
        mDao.update(entity);
    }

    @Override
    public SegmentChangeDTO getSnapshot(String userKey) {
        String encryptedUserKey = mSplitCipher.encrypt(userKey);
        return getMySegmentsFromEntity(mDao.getByUserKey(encryptedUserKey));
    }

    @Override
    public void close() {
    }

    private SegmentChangeDTO getMySegmentsFromEntity(SegmentEntity entity) {
        if (entity == null || Utils.isNullOrEmpty(entity.getSegmentList())) {
            return createEmpty();
        }

        String storedJson = mSplitCipher.decrypt(entity.getSegmentList());
        if (storedJson == null) {
            return createEmpty();
        }

        SegmentChangeDTO dto;
        try {
            dto = Json.fromJson(storedJson, SegmentChangeDTO.class);

            Logger.v("Returning segments from DTO");
            return dto;
        } catch (JsonParseException | NullPointerException ex) {
            Logger.v("Parsing of segments DTO failed, returning as legacy");
            String[] segments = storedJson.split(",");

            return new SegmentChangeDTO(Arrays.asList(segments), -1L);
        }
    }
}
