package io.split.android.client.localhost;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.security.AlgorithmConstraints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Algorithm;
import io.split.android.client.dtos.Condition;
import io.split.android.client.dtos.ConditionType;
import io.split.android.client.dtos.Matcher;
import io.split.android.client.dtos.MatcherCombiner;
import io.split.android.client.dtos.MatcherGroup;
import io.split.android.client.dtos.MatcherType;
import io.split.android.client.dtos.Partition;
import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;
import io.split.android.client.dtos.WhitelistMatcherData;
import io.split.android.client.storage.legacy.IStorage;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.YamlParser;
import io.split.android.grammar.Treatments;

public class LocalhostYamlFileParser implements LocalhostFileParser {

    private static final String TREATMENT_FIELD = "treatment";
    private static final String CONFIG_FIELD = "config";
    private static final String KEYS_FIELD = "keys";

    @Override
    public Map<String, Split> parse(String content) {
         Map<String, Split> splits = null;

        YamlParser parser = new YamlParser();
        try {
            List<Object> loadedSplits = parser.parse(content);
            if(loadedSplits == null) {
                Logger.e("Split file could not be parser because it is not in the correct format.");
                return null;
            }

            splits = new HashMap<>();
            for (Object loadedSplit : loadedSplits) {
                Map<String, Object> parsedSplit = (Map<String, Object>) loadedSplit;
                Object[] splitNameContainer = parsedSplit.keySet().toArray();
                if (splitNameContainer != null && splitNameContainer.length > 0) {
                    String splitName = (String) splitNameContainer[0];
                    if (splitName == null) {
                        continue;
                    }
                    Map<String, String> splitMap = (Map<String, String>) parsedSplit.get(splitName);
                    if (splitMap == null) {
                        continue;
                    }

                    Split split = splits.get(splitName);
                    if (split == null) {
                        split = SplitHelper.createDefaultSplit(splitName);
                    }

                    String treatment = splitMap.get(TREATMENT_FIELD);
                    if (treatment == null) {
                        continue;
                    }
                    List<String> keys = parseKeys(splitMap.get(KEYS_FIELD));
                    if (keys.size() > 0) {
                        split.conditions.add(0, SplitHelper.createWhiteListCondition(keys, treatment));
                    } else {
                        split.conditions.add(SplitHelper.createRolloutCondition(treatment));
                    }

                    String config = splitMap.get(CONFIG_FIELD);
                    if (config != null) {
                        if (split.configurations == null) {
                            split.configurations = new HashMap<>();
                        }
                        split.configurations.put(treatment, config);
                    }
                    splits.put(split.name, split);
                }
            }
        } catch (Exception e) {
            Logger.e("An error has ocurred while parsing localhost splits content");
        }
        return splits;
    }

    private @NonNull List<String> parseKeys(@Nullable Object keysContent) {
        if(keysContent == null) {
            return new ArrayList<>();
        }

        List<String> keys = null;
        try {
            if(keysContent instanceof List) {
                keys = (ArrayList<String>) keysContent;
                return keys;
            } else {
                keys = new ArrayList<>();
                keys.add((String) keysContent);
            }
        } catch (ClassCastException ignored) {
        }
        return keys;
    }
}