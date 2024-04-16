package io.split.android.engine.matchers.semver;

import java.util.Map;

import io.split.android.client.Evaluator;
import io.split.android.client.utils.logger.Logger;
import io.split.android.engine.matchers.Matcher;

public class LessThanOrEqualToSemverMatcher implements Matcher {

    private final Semver mTarget;

    public LessThanOrEqualToSemverMatcher(String target) {
        mTarget = Semver.build(target);
    }

    @Override
    public boolean match(Object key, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        if (key == null || mTarget == null) {
            return false;
        }

        if (!(key instanceof String)) {
            return false;
        }

        Semver keySemver = Semver.build((String) key);
        if (keySemver == null) {
            return false;
        }

        boolean result = keySemver.compare(mTarget) <= 0;

        Logger.d(keySemver.getVersion() +" <= " + mTarget.getVersion() + " | Result: " + result);

        return result;
    }
}
