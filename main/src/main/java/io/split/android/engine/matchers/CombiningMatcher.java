package io.split.android.engine.matchers;

import static io.split.android.client.utils.Utils.checkArgument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import io.split.android.client.Evaluator;
import io.split.android.client.dtos.MatcherCombiner;

/**
 * Combines the results of multiple matchers using the logical OR or AND.
 *
 */
public class CombiningMatcher {

    private final List<AttributeMatcher> _delegates;
    private final MatcherCombiner _combiner;

    public static CombiningMatcher of(Matcher matcher) {
        return new CombiningMatcher(MatcherCombiner.AND,
                Collections.singletonList(AttributeMatcher.vanilla(matcher)));
    }

    public static CombiningMatcher of(String attribute, Matcher matcher) {
        return new CombiningMatcher(MatcherCombiner.AND,
                Collections.singletonList(new AttributeMatcher(attribute, matcher, false)));
    }

    public CombiningMatcher(MatcherCombiner combiner, List<AttributeMatcher> delegates) {
        _delegates = Collections.unmodifiableList(new ArrayList<>(delegates));
        _combiner = combiner;

        checkArgument(_delegates.size() > 0);
    }

    public boolean match(String key, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        if (_delegates.isEmpty()) {
            return false;
        }

        if (_combiner == MatcherCombiner.AND) {
            return and(key, bucketingKey, attributes, evaluator);
        }
        throw new IllegalArgumentException("Unknown combiner: " + _combiner);

    }

    private boolean and(String key, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        boolean result = true;
        for (AttributeMatcher delegate : _delegates) {
            result &= (delegate.match(key, bucketingKey, attributes, evaluator));
        }
        return result;
    }

    public List<AttributeMatcher> attributeMatchers() {
        return _delegates;
    }

    @Override
    public int hashCode() {
        int result = _delegates.hashCode();
        result = 31 * result + _combiner.hashCode();
        return result;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("if");
        boolean first = true;
        for (AttributeMatcher matcher : _delegates) {
            if (!first) {
                bldr.append(" ").append(_combiner);
            }
            bldr.append(" ");
            bldr.append(matcher);
            first = false;
        }
        return bldr.toString();
    }


    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (!(obj instanceof CombiningMatcher)) return false;

        CombiningMatcher other = (CombiningMatcher) obj;

        return _combiner.equals(other._combiner) && _delegates.equals(other._delegates);
    }
}
