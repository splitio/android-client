package io.split.android.engine.matchers;

import io.split.android.client.Evaluator;

import java.util.Map;

public final class AttributeMatcher {

    private final String _attribute;
    private final Matcher _matcher;


    public static AttributeMatcher vanilla(Matcher matcher) {
        return new AttributeMatcher(null, matcher, false);
    }

    public AttributeMatcher(String attribute, Matcher matcher, boolean negate) {
        _attribute = attribute;
        if (matcher == null) {
            throw new IllegalArgumentException("Null matcher");
        }
        _matcher = new NegatableMatcher(matcher, negate);
    }

    public boolean match(String key, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        if (_attribute == null) {
            return _matcher.match(key, bucketingKey, attributes, evaluator);
        }

        if (attributes == null) {
            return false;
        }

        Object value = attributes.get(_attribute);
        if (value == null) {
            return false;
        }


        return _matcher.match(value, bucketingKey, null, null);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AttributeMatcher that = (AttributeMatcher) o;

        if (_attribute != null ? !_attribute.equals(that._attribute) : that._attribute != null)
            return false;
        return _matcher.equals(that._matcher);
    }

    @Override
    public int hashCode() {
        int result = _attribute != null ? _attribute.hashCode() : 0;
        result = 31 * result + _matcher.hashCode();
        return result;
    }

    public String attribute() {
        return _attribute;
    }

    public Matcher matcher() {
        return _matcher;
    }

    @Override
    public String toString() {
        StringBuilder bldr = new StringBuilder();
        bldr.append("key");
        if (_attribute != null) {
            bldr.append(".");
            bldr.append(_attribute);
        }

        bldr.append(" is");
        bldr.append(_matcher);
        return bldr.toString();
    }

    public static final class NegatableMatcher implements Matcher {
        private final boolean _negate;
        private final Matcher _delegate;

        public NegatableMatcher(Matcher matcher, boolean negate) {
            _negate = negate;
            _delegate = matcher;
        }


        @Override
        public boolean match(Object matchValue, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
            boolean result = _delegate.match(matchValue, bucketingKey, attributes, evaluator);
            return (_negate) ? !result : result;
        }

        @Override
        public String toString() {
            StringBuilder bldr = new StringBuilder();
            if (_negate) {
                bldr.append(" not");
            }
            bldr.append(" ");
            bldr.append(_delegate);
            return bldr.toString();
        }

        public Matcher delegate() {
            return _delegate;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            NegatableMatcher that = (NegatableMatcher) o;

            if (_negate != that._negate) return false;
            return _delegate.equals(that._delegate);
        }

        @Override
        public int hashCode() {
            int result = (_negate ? 1 : 0);
            result = 31 * result + _delegate.hashCode();
            return result;
        }
    }

}
