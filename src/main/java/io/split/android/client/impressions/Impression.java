package io.split.android.client.impressions;

import androidx.annotation.Nullable;

import java.util.Map;

public class Impression {
    
    private final String _key;
    private final String _bucketingKey;
    private final String _split;
    private final String _treatment;
    private final long _time;
    private final String _appliedRule;
    private final Long _changeNumber;
    private Long _previousTime;
    private final Map<String, Object> _attributes;
    @Nullable
    private final String _propertiesJson;


    public Impression(String key, String bucketingKey, String split, String treatment, long time, String appliedRule, Long changeNumber, Map<String, Object> attributes, String propertiesJson) {
        _key = key;
        _bucketingKey = bucketingKey;
        _split = split;
        _treatment = treatment;
        _time = time;
        _appliedRule = appliedRule;
        _changeNumber = changeNumber;
        _attributes = attributes;
        _propertiesJson = propertiesJson;
    }

    public String key() {
        return _key;
    }

    public String bucketingKey() {
        return _bucketingKey;
    }

    public String split() {
        return _split;
    }

    public String treatment() {
        return _treatment;
    }

    public long time() {
        return _time;
    }

    public String appliedRule() {
        return _appliedRule;
    }

    public Long changeNumber() {
        return _changeNumber;
    }

    public Map<String, Object> attributes() {
        return _attributes;
    }

    @Nullable
    public String properties() {
        return _propertiesJson;
    }

    public Long previousTime() {
        return _previousTime;
    }

    public Impression withPreviousTime(Long pt) { _previousTime = pt; return this; }
}
