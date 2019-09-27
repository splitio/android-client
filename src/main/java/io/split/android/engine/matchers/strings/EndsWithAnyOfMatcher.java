package io.split.android.engine.matchers.strings;

import io.split.android.client.Evaluator;
import io.split.android.engine.matchers.Matcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class EndsWithAnyOfMatcher implements Matcher {

    private final Set<String> _compareTo = new HashSet<>();

    public EndsWithAnyOfMatcher(Collection<String> compareTo) {
        if (compareTo == null) {
            throw new IllegalArgumentException("Null whitelist");
        }
        _compareTo.addAll(compareTo);
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {

        if (matchValue == null) {
            return false;
        }

        if (!(matchValue instanceof String) ) {
            return false;
        }

        if (_compareTo.isEmpty()) {
            return false;
        }

        String keyAsString = (String) matchValue;

        for (String s : _compareTo) {
            if (s.isEmpty()) {
                // ignore empty strings.
                continue;
            }
            if (keyAsString.endsWith(s)) {
                return true;
            }
        }

        return false;
    }



    @Override
    public String toString() {
        return "ends with " +
                _compareTo;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + _compareTo.hashCode();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof EndsWithAnyOfMatcher)) return false;

        EndsWithAnyOfMatcher other = (EndsWithAnyOfMatcher) obj;

        return _compareTo.equals(other._compareTo);
    }

}
