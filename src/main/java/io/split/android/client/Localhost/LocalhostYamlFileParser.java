package io.split.android.client.Localhost;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import io.split.android.client.dtos.Split;
import io.split.android.client.storage.IStorage;
import io.split.android.client.utils.Logger;
import io.split.android.client.utils.YamlParser;
import io.split.android.grammar.Treatments;

public class LocalhostYamlFileParser implements LocalhostFileParser {

    private static final String TREATMENT_FIELD = "treatment";
    private static final String CONFIG_FIELD = "config";
    private static final String KEYS_FIELD = "keys";

    private IStorage mFileStorage;
    private LocalhostGrammar mLocalhostGrammar;

    public LocalhostYamlFileParser(IStorage fileStorage) {
        mFileStorage = fileStorage;
        mLocalhostGrammar = new LocalhostGrammar();
    }

    @Override
    public Map<String, Split> parse(String fileName) {
         Map<String, Split> splits = null;
        String content = null;

        try {
            content = mFileStorage.read(fileName);
        } catch (IOException e) {
            Logger.e("Error reading localhost yaml file");
            return null;
        }

        YamlParser parser = new YamlParser();
        try {
            List<Object> loadedSplits = (List<Object>) parser.parse(content);
            if(loadedSplits == null) {
                Logger.e("Split file could not be parser because it is not in the correct format.");
                return null;
            }

            splits = new HashMap<>();
            for (Object loadedSplit : loadedSplits) {
                Map<String, Object> parsedSplit = (Map<String, Object>) loadedSplit;
                Object[] splitNameContainer = parsedSplit.keySet().toArray();
                if (splitNameContainer.length > 0) {
                    String splitName = (String) splitNameContainer[0];
                    Map<String, String> splitMap = (Map<String, String>) parsedSplit.get(splitName);

                    List<String> keys = parseKeys(splitMap.get(KEYS_FIELD));
                    int count = (keys != null ? keys.size() : 1);
                    for(int i = 0; i < count; i++) {
                        Split split = new Split();
                        String key = (keys != null ? keys.get(i) : null);
                        split.name = mLocalhostGrammar.buildSplitKeyName(splitName, key);
                        split.defaultTreatment = splitMap.get(TREATMENT_FIELD);

                        if (split.defaultTreatment == null) {
                            Logger.e("Parsing Localhost Split " + split.name + "does not have a treatment value. Using control");
                            split.defaultTreatment = Treatments.CONTROL;
                        }
                        String config = splitMap.get(CONFIG_FIELD);
                        if (config != null) {
                            Map<String, String> configs = new HashMap<>();
                            configs.put(split.defaultTreatment, config);
                            split.configurations = configs;
                        }
                        splits.put(split.name, split);
                    }
                }
            }
        } catch (Exception e) {
            Logger.e("An error has ocurred while parsing localhost splits content");
        }
        return splits;
    }

    private List<String> parseKeys(Object keysContent) {
        if(keysContent == null) {
            return null;
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
