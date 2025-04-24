package io.split.android.client.storage.splits;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.split.android.client.dtos.Split;

class MetadataHelper {

    static void increaseTrafficTypeCount(@Nullable String name, Map<String, Integer> outputTrafficTypes) {
        if (name == null) {
            return;
        }

        String lowercaseName = name.toLowerCase();
        int count = countForTrafficType(lowercaseName, outputTrafficTypes);
        outputTrafficTypes.put(lowercaseName, ++count);
    }

    static void decreaseTrafficTypeCount(@Nullable String name, Map<String, Integer> outputTrafficTypes) {
        if (name == null) {
            return;
        }
        String lowercaseName = name.toLowerCase();

        int count = countForTrafficType(lowercaseName, outputTrafficTypes);
        if (count > 1) {
            outputTrafficTypes.put(lowercaseName, --count);
        } else {
            outputTrafficTypes.remove(lowercaseName);
        }
    }

    static int countForTrafficType(@NonNull String name, Map<String, Integer> outputTrafficTypes) {
        int count = 0;
        Integer countValue = outputTrafficTypes.get(name);
        if (countValue != null) {
            count = countValue;
        }
        return count;
    }

    static void addOrUpdateFlagSets(Split split, Map<String, Set<String>> outputFlagSets) {
        if (split.sets == null) {
            return;
        }

        for (String set : split.sets) {
            Set<String> splitsForSet = outputFlagSets.get(set);
            if (splitsForSet == null) {
                splitsForSet = new HashSet<>();
                outputFlagSets.put(set, splitsForSet);
            }
            splitsForSet.add(split.name);
        }

        deleteFromFlagSetsIfNecessary(split, outputFlagSets);
    }

    static void deleteFromFlagSetsIfNecessary(Split featureFlag, Map<String, Set<String>> outputFlagSets) {
        if (featureFlag.sets == null) {
            return;
        }

        for (String set : outputFlagSets.keySet()) {
            if (featureFlag.sets.contains(set)) {
                continue;
            }

            Set<String> flagsForSet = outputFlagSets.get(set);
            if (flagsForSet != null) {
                flagsForSet.remove(featureFlag.name);
            }
        }
    }

    static void deleteFromFlagSets(Split featureFlag, Map<String, Set<String>> outputFlagSets) {
        for (String set : outputFlagSets.keySet()) {
            Set<String> flagsForSet = outputFlagSets.get(set);
            if (flagsForSet != null) {
                flagsForSet.remove(featureFlag.name);
            }
        }
    }
}
