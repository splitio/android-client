package io.split.android.engine.matchers;

import io.split.android.client.Evaluator;
import io.split.android.client.SplitClientImpl;

import java.util.Map;

public class BooleanMatcher implements Matcher {
    private boolean _booleanValue;

    public BooleanMatcher(boolean booleanValue) {
        _booleanValue = booleanValue;
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        if (matchValue == null) {
            return false;
        }

        Boolean valueAsBoolean = Transformers.asBoolean(matchValue);

        return valueAsBoolean != null && valueAsBoolean == _booleanValue;
    }

    @Override
    public String toString() {
        return "is " + Boolean.toString(_booleanValue);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BooleanMatcher that = (BooleanMatcher) o;

        return _booleanValue == that._booleanValue;
    }

    @Override
    public int hashCode() {
        return (_booleanValue ? 1 : 0);
    }
}
