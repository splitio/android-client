package io.split.android.client;

import java.util.Set;

interface FlagSetsFilter {

    boolean intersect(Set<String> sets);

    boolean intersect(String set);
}
