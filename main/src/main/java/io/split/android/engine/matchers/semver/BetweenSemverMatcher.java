package io.split.android.engine.matchers.semver;

import java.util.Map;

import io.split.android.client.Evaluator;
import io.split.android.client.utils.logger.Logger;
import io.split.android.engine.matchers.Matcher;

public class BetweenSemverMatcher implements Matcher {

    private final Semver mStartTarget;
    private final Semver mEndTarget;

    public BetweenSemverMatcher(String start, String end) {
        mStartTarget = Semver.build(start);
        mEndTarget = Semver.build(end);
    }

    @Override
    public boolean match(Object key, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        if (key == null || mStartTarget == null || mEndTarget == null) {
            return false;
        }

        if (!(key instanceof String)) {
            return false;
        }

        Semver keySemver = Semver.build((String) key);
        if (keySemver == null) {
            return false;
        }

        boolean result = keySemver.compare(mStartTarget) >= 0 && keySemver.compare(mEndTarget) <= 0;

        Logger.d(mStartTarget.getVersion() + " <= " + keySemver.getVersion() + " <= " + mEndTarget.getVersion() + " | Result: " + result);

        return result;
    }
}
