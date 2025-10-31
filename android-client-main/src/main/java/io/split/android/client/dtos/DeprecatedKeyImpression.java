package io.split.android.client.dtos;


public class DeprecatedKeyImpression {

    public transient long storageId;
    public String feature;
    public String keyName;
    public String bucketingKey;
    public String treatment;
    public String label;
    public long time;
    public Long changeNumber; // can be null if there is no changeNumber

    public DeprecatedKeyImpression() {
    }
}
