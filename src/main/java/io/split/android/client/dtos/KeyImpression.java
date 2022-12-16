package io.split.android.client.dtos;


import com.google.gson.annotations.SerializedName;

import io.split.android.client.service.ServiceConstants;
import io.split.android.client.storage.common.InBytesSizable;
import io.split.android.client.impressions.Impression;

public class KeyImpression implements InBytesSizable, Identifiable {

    public transient long storageId;
    /* package private */ static final String FIELD_KEY_NAME = "k";
    /* package private */ static final String FIELD_BUCKETING_KEY = "b";
    /* package private */ static final String FIELD_TREATMENT = "t";
    /* package private */ static final String FIELD_LABEL = "r";
    /* package private */ static final String FIELD_TIME = "m";
    /* package private */ static final String FIELD_CHANGE_NUMBER = "c";
    /* package private */ static final String FIELD_PREVIOUS_TIME = "pt";

    public transient String feature; // Non-serializable

    @SerializedName(FIELD_KEY_NAME)
    public String keyName;

    @SerializedName(FIELD_BUCKETING_KEY)
    public String bucketingKey;

    @SerializedName(FIELD_TREATMENT)
    public String treatment;

    @SerializedName(FIELD_LABEL)
    public String label;

    @SerializedName(FIELD_TIME)
    public long time;

    @SerializedName(FIELD_CHANGE_NUMBER)
    public Long changeNumber; // can be null if there is no changeNumber

    @SerializedName(FIELD_PREVIOUS_TIME)
    public Long previousTime;

    public KeyImpression() {
    }

    public KeyImpression(Impression impression) {
        this.feature = impression.split();
        this.keyName = impression.key();
        this.bucketingKey = impression.bucketingKey();
        this.label = impression.appliedRule();
        this.treatment = impression.treatment();
        this.time = impression.time();
        this.changeNumber = impression.changeNumber();
        this.previousTime = impression.previousTime();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        KeyImpression that = (KeyImpression) o;

        if (time != that.time) return false;
        if (feature != null ? !feature.equals(that.feature) : that.feature != null) return false;
        if (!keyName.equals(that.keyName)) return false;
        if (!treatment.equals(that.treatment)) return false;

        if (bucketingKey == null) {
            return that.bucketingKey == null;
        }
        if (!previousTime.equals(that.previousTime)) return false;

        return bucketingKey.equals(that.bucketingKey);
    }

    @Override
    public long getSizeInBytes() {
        return ServiceConstants.ESTIMATED_IMPRESSION_SIZE_IN_BYTES;
    }

    @Override
    public int hashCode() {
        int result = feature != null ? feature.hashCode() : 0;
        result = 31 * result + keyName.hashCode();
        result = 31 * result + (bucketingKey == null ? 0 : bucketingKey.hashCode());
        result = 31 * result + treatment.hashCode();
        result = 31 * result + (int) (time ^ (time >>> 32));
        result = 31 * result + previousTime.hashCode();
        return result;
    }

    public static KeyImpression fromImpression(Impression impression) {
        KeyImpression keyImpression = new KeyImpression();
        keyImpression.feature = impression.split();
        keyImpression.keyName = impression.key();
        keyImpression.bucketingKey = impression.bucketingKey();
        keyImpression.time = impression.time();
        keyImpression.changeNumber = impression.changeNumber();
        keyImpression.treatment = impression.treatment();
        keyImpression.label = impression.appliedRule();
        keyImpression.previousTime = impression.previousTime();
        return keyImpression;
    }

    @Override
    public long getId() {
        return storageId;
    }
}
