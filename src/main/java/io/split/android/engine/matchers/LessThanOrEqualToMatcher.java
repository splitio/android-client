package io.split.android.engine.matchers;

import io.split.android.client.Evaluator;
import io.split.android.client.dtos.DataType;

import java.util.Map;

public class LessThanOrEqualToMatcher implements Matcher {
    private final long _compareTo;
    private final long _normalizedCompareTo;
    private final DataType _dataType;

    public LessThanOrEqualToMatcher(long compareTo, DataType dataType) {
        _compareTo = compareTo;
        _dataType = dataType;

        if (_dataType == DataType.DATETIME) {
            _normalizedCompareTo = Transformers.asDateHourMinute(_compareTo);
        } else {
            _normalizedCompareTo = _compareTo;
        }
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        Long keyAsLong;

        if (_dataType == DataType.DATETIME) {
            keyAsLong = Transformers.asDateHourMinute(matchValue);
        } else {
            keyAsLong = Transformers.asLong(matchValue);
        }

        if (keyAsLong == null) {
            return false;
        }

        return keyAsLong <= _normalizedCompareTo;
    }

    @Override
    public String toString() {
        return "<= " +
                _compareTo;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (int)(_compareTo ^ (_compareTo >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof LessThanOrEqualToMatcher)) return false;

        LessThanOrEqualToMatcher other = (LessThanOrEqualToMatcher) obj;

        return _compareTo == other._compareTo;
    }

}
