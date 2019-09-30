package io.split.android.engine.matchers;

import io.split.android.client.Evaluator;
import io.split.android.client.dtos.DataType;

import java.util.Map;

import static io.split.android.engine.matchers.Transformers.asDateHourMinute;
import static io.split.android.engine.matchers.Transformers.asLong;

public class GreaterThanOrEqualToMatcher implements Matcher {

    private final long _compareTo;
    private final long _normalizedCompareTo;
    private final DataType _dataType;

    public GreaterThanOrEqualToMatcher(long compareTo, DataType dataType) {
        _compareTo = compareTo;
        _dataType = dataType;

        if (_dataType == DataType.DATETIME) {
            //noinspection ConstantConditions
            _normalizedCompareTo = asDateHourMinute(_compareTo);
        } else {
            _normalizedCompareTo = _compareTo;
        }
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        Long keyAsLong;

        if (_dataType == DataType.DATETIME) {
            keyAsLong = asDateHourMinute(matchValue);
        } else {
            keyAsLong = asLong(matchValue);
        }

        if (keyAsLong == null) {
            return false;
        }

        return keyAsLong >= _normalizedCompareTo;
    }

    @Override
    public String toString() {
        return ">= " +
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
        if (!(obj instanceof GreaterThanOrEqualToMatcher)) return false;

        GreaterThanOrEqualToMatcher other = (GreaterThanOrEqualToMatcher) obj;

        return _compareTo == other._compareTo;
    }

}
