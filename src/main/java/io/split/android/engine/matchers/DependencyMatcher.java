package io.split.android.engine.matchers;

import io.split.android.client.EvaluationResult;
import io.split.android.client.Evaluator;

import java.util.List;
import java.util.Map;

/**
 * Supports the logic: if user is in split "feature" treatments ["on","off"]
 */
public class DependencyMatcher implements Matcher {
    private String _split;
    private List<String> _treatments;

    public DependencyMatcher(String split, List<String> treatments) {
        _split = split;
        _treatments = treatments;
    }

    @Override
    public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        if (matchValue == null) {
            return false;
        }

        if (!(matchValue instanceof String)) {
            return false;
        }

        EvaluationResult result = evaluator.getTreatment(
                (String) matchValue,
                bucketingKey,
                _split,
                attributes
        );

        return _treatments.contains(result.getTreatment());
    }

    @Override
    public String toString() {
        return "in split \"" +
                this._split +
                "\" treatment " +
                this._treatments;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DependencyMatcher that = (DependencyMatcher) o;

        if (_split != null ? !_split.equals(that._split) : that._split != null) return false;
        return _treatments != null ? _treatments.equals(that._treatments) : that._treatments == null;
    }

    @Override
    public int hashCode() {
        int result = _split != null ? _split.hashCode() : 0;
        result = 31 * result + (_treatments != null ? _treatments.hashCode() : 0);
        return result;
    }
}
