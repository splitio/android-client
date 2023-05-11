package io.split.android.client.localhost;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Split;
import io.split.android.client.utils.logger.Logger;
import io.split.android.client.utils.YamlParser;

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
            if (loadedSplits == null) {
                Logger.e("Feature flag file could not be parsed because it is not in the correct format.");
                return null;
            }

            splits = new HashMap<>();
            for (Object loadedSplit : loadedSplits) {
                try {
                    addLoadedSplitToParsedSplits(splits, (Map<String, Object>) loadedSplit);
                } catch (Exception exception) {
                    Logger.e("An error has occurred while parsing a feature flag" + (loadedSplit != null ? (", source: '" + loadedSplit + "'") : ""));
                }
            }
        } catch (Exception e) {
            Logger.e("An error has occurred while parsing localhost feature flags content");
        }
        return splits;
    }

    private void addLoadedSplitToParsedSplits(Map<String, Split> splits, Map<String, Object> loadedSplit) {
        Object[] splitNameContainer = loadedSplit.keySet().toArray();
        if (splitNameContainer.length > 0) {
            String splitName = (String) splitNameContainer[0];
            if (splitName == null) {
                return;
            }

            Map<String, String> splitMap = (Map<String, String>) loadedSplit.get(splitName);
            if (splitMap == null || splitMap.get(TREATMENT_FIELD) == null) {
                return;
            }

            Split split = getOrCreateSplit(splits, splitName);
            String treatment = splitMap.get(TREATMENT_FIELD);

            addConditionsToSplit(split, treatment, parseKeys(splitMap.get(KEYS_FIELD)));
            addConfigToSplit(split, splitMap, treatment);

            splits.put(split.name, split);
        }
    }

    private void addConfigToSplit(Split split, Map<String, String> splitMap, String treatment) {
        String config = splitMap.get(CONFIG_FIELD);
        if (config != null) {
            if (split.configurations == null) {
                split.configurations = new HashMap<>();
            }
            split.configurations.put(treatment, config);
        }
    }

    private void addConditionsToSplit(Split split, String treatment, List<String> keys) {
        if (keys.size() > 0) {
            split.conditions.add(0, SplitHelper.createWhiteListCondition(keys, treatment));
        } else {
            split.conditions.add(SplitHelper.createRolloutCondition(treatment));
        }
    }

    @NonNull
    private List<String> parseKeys(@Nullable Object keysContent) {
        if (keysContent == null) {
            return new ArrayList<>();
        }

        List<String> keys = new ArrayList<>();
        try {
            if (keysContent instanceof List) {
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

    @NonNull
    private Split getOrCreateSplit(Map<String, Split> splits, String splitName) {
        Split split = splits.get(splitName);
        if (split == null) {
            split = SplitHelper.createDefaultSplit(splitName);
        }

        return split;
    }
}
