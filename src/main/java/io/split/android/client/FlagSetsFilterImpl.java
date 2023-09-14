package io.split.android.client;

import java.util.HashSet;
import java.util.Set;

class FlagSetsFilterImpl implements FlagSetsFilter {

    private final boolean mShouldFilter;
    private final Set<String> mFlagSets;

    FlagSetsFilterImpl(Set<String> flagSets) {
        mFlagSets = new HashSet<>(flagSets);
        mShouldFilter = !mFlagSets.isEmpty();
    }

    @Override
    public boolean intersect(Set<String> sets) {
        if (!mShouldFilter) {
            return true;
        }

        if (sets == null) {
            return false;
        }

        for (String set : sets) {
            if (mFlagSets.contains(set)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean intersect(String set) {
        if (!mShouldFilter) {
            return true;
        }

        if (set == null) {
            return false;
        }

        return mFlagSets.contains(set);
    }
}
