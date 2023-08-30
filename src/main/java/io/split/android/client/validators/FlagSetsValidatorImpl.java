package io.split.android.client.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

import io.split.android.client.utils.logger.Logger;

public class FlagSetsValidatorImpl implements SplitFilterValidator {

    private static final String FLAG_SET_REGEX = "^[a-z0-9][_a-z0-9]{0,49}$";

    /**
     * Validates the flag sets and returns a list of
     * de-duplicated and alphanumerically ordered valid flag sets.
     *
     * @param values list of flag sets
     * @return list of unique alphanumerically ordered valid flag sets
     */
    @Override
    public List<String> cleanup(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }

        TreeSet<String> cleanedUpSets = new TreeSet<>();
        for (String set : values) {
            if (set == null || set.isEmpty()) {
                continue;
            }

            if (set.trim().length() != set.length()) {
                Logger.w("SDK config: Flag Set name " + set + " has extra whitespace, trimming");
                set = set.trim();
            }

            if (!set.toLowerCase().equals(set)) {
                Logger.w("SDK config: Flag Set name "+set+" should be all lowercase - converting string to lowercase");
                set = set.toLowerCase();
            }

            if (set.matches(FLAG_SET_REGEX)) {
                cleanedUpSets.add(set);
            } else {
                Logger.w("SDK config: you passed "+ set +", Flag Set must adhere to the regular expressions "+ FLAG_SET_REGEX +". This means a Flag Set must be start with a letter, be in lowercase, alphanumeric and have a max length of 50 characters. "+ set +" was discarded.");
            }
        }

        return new ArrayList<>(cleanedUpSets);
    }
}
