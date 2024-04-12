package io.split.android.engine.matchers.semver;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.split.android.client.Evaluator;
import io.split.android.engine.matchers.Matcher;

public class InListSemverMatcher implements Matcher {

    private final Set<Semver> mTargetList = new HashSet<>();

    public InListSemverMatcher(List<String> targetList) {
        if (targetList != null) {
            for (String item : targetList) {
                Semver toAdd = Semver.build(item);
                if (toAdd != null) {
                    mTargetList.add(toAdd);
                }
            }
        }
    }

    @Override
    public boolean match(Object key, String bucketingKey, Map<String, Object> attributes, Evaluator evaluator) {
        if (key == null || mTargetList.isEmpty()) {
            return false;
        }

        if (!(key instanceof String)) {
            return false;
        }

        Semver keySemver = Semver.build((String) key);
        if (keySemver == null) {
            return false;
        }

        for (Semver item : mTargetList) {
            if (keySemver.equals(item)) {
                return true;
            }
        }

        return false;
    }
}
