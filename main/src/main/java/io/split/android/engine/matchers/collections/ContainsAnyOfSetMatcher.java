package io.split.android.engine.matchers.collections;

import io.split.android.client.Evaluator;
import io.split.android.engine.matchers.Matcher;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.split.android.engine.matchers.Transformers.toSetOfStrings;

public class ContainsAnyOfSetMatcher implements Matcher {

    private final Set<String> _compareTo = new HashSet<>();

    public ContainsAnyOfSetMatcher(Collection<String> compareTo) {
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

        if (!(matchValue instanceof Collection)) {
            return false;
        }

        Set<String> keyAsSet = toSetOfStrings((Collection) matchValue);

        for (String s : _compareTo) {
            if ((keyAsSet.contains(s))) {
                return true;
            }
        }

        return false;
    }


    @Override
    public String toString() {
        return "contains any of " + _compareTo;
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
        if (!(obj instanceof ContainsAnyOfSetMatcher)) return false;

        ContainsAnyOfSetMatcher other = (ContainsAnyOfSetMatcher) obj;

        return _compareTo.equals(other._compareTo);
    }

}
