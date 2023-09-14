package io.split.android.client;

import java.util.Set;

interface FeatureFlagFilter {

    boolean intersect(Set<String> values);

    boolean intersect(String values);
}
