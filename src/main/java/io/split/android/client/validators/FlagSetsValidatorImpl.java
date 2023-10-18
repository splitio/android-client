package io.split.android.client.validators;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import io.split.android.client.FlagSetsFilter;
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
    public ValidationResult cleanup(String method, List<String> values) {
        if (values == null || values.isEmpty()) {
            return new ValidationResult(Collections.emptyList(), 0);
        }

        int invalidValueCount = 0;

        TreeSet<String> cleanedUpSets = new TreeSet<>();
        for (String set : values) {
            if (set == null || set.isEmpty()) {
                invalidValueCount++;
                continue;
            }

            if (set.trim().length() != set.length()) {
                Logger.w(method + ": Flag Set name " + set + " has extra whitespace, trimming");
                set = set.trim();
            }

            if (!set.toLowerCase().equals(set)) {
                Logger.w(method + ": Flag Set name "+set+" should be all lowercase - converting string to lowercase");
                set = set.toLowerCase();
            }

            if (set.matches(FLAG_SET_REGEX)) {
                if (!cleanedUpSets.add(set)) {
                    Logger.w(method + ": you passed duplicated Flag Set. " + set + " was deduplicated");
                    invalidValueCount++;
                }
            } else {
                invalidValueCount++;
                Logger.w(method + ": you passed "+ set +", Flag Set must adhere to the regular expressions "+ FLAG_SET_REGEX +". This means a Flag Set must be start with a letter, be in lowercase, alphanumeric and have a max length of 50 characters. "+ set +" was discarded.");
            }
        }

        return new ValidationResult(new ArrayList<>(cleanedUpSets), invalidValueCount);
    }

    @Override
    public boolean isValid(String value) {
        return value != null && value.trim().matches(FLAG_SET_REGEX);
    }

    @Override
    public Set<String> items(List<String> values, FlagSetsFilter flagSetsFilter) {
        Set<String> setsToReturn = new HashSet<>();

        if (values == null || values.isEmpty()) {
            return setsToReturn;
        }

        for (String flagSet : values) {
            if (!isValid(flagSet)) {
                continue;
            }

            if (flagSetsFilter != null && !flagSetsFilter.intersect(flagSet)) {
                continue;
            }

            setsToReturn.add(flagSet);
        }

        return setsToReturn;
    }
}
