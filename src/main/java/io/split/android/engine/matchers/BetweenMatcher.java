package io.split.android.engine.matchers;

import io.split.android.client.Evaluator;
import io.split.android.client.dtos.DataType;

import java.util.Map;

/**
 * Supports the logic: if user.age is between x and y
 *
 */
public class BetweenMatcher implements Matcher {
    private final long _start;
    private final long _end;
    private final long _normalizedStart;
    private final long _normalizedEnd;

    private final DataType _dataType;

    @SuppressWarnings("ConstantConditions")
    public BetweenMatcher(long start, long end, DataType dataType) {
        _start = start;
        _end = end;
        _dataType = dataType;

        if (_dataType == DataType.DATETIME) {
            _normalizedStart = Transformers.asDateHourMinute(_start);
            _normalizedEnd = Transformers.asDateHourMinute(_end);
        } else {
            _normalizedStart = _start;
            _normalizedEnd = _end;
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

        return keyAsLong >= _normalizedStart && keyAsLong <= _normalizedEnd;
    }

    @Override
    public String toString() {
        return "between " + _start + " and " + _end;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + (int)(_start ^ (_start >>> 32));
        result = 31 * result + (int)(_end ^ (_end >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof BetweenMatcher)) return false;

        BetweenMatcher other = (BetweenMatcher) obj;

        return _start == other._start && _end == other._end;
    }

}
