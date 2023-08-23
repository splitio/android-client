package io.split.android.client.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import io.split.android.client.utils.logger.Logger;

public class FlagSetsValidatorImpl implements SplitFilterValidator {

    private static final String FLAG_SET_REGEX = "^[a-z][_a-z0-9]{1,49}$";


    /**
     * Validates the flag sets and returns a list of
     * de-duplicated and alphanumerically ordered valid flag sets.
     *
     * @param sets list of flag sets
     * @return list of unique alphanumerically ordered valid flag sets
     */
    @Override
    public List<String> cleanup(List<String> sets) {
        if (sets == null || sets.isEmpty()) {
            return Collections.emptyList();
        }

        TreeSet<String> cleanedUpSets = new TreeSet<>();
        for (String set : sets) {
            if (set == null || set.isEmpty()) {
                continue;
            }

            if (set.startsWith(" ") || set.endsWith(" ")) {
                Logger.w("SDK config: Flag Set name " + set + " has extra whitespace, trimming");
                set = set.trim();
            }

            if (set.matches(FLAG_SET_REGEX)) {
                cleanedUpSets.add(set);
            }
        }

        return new ArrayList<>(cleanedUpSets);
    }
}
