package io.split.android.client.localhost;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import io.split.android.client.dtos.Split;
import io.split.android.client.utils.Logger;

public class LocalhostPropertiesFileParser implements LocalhostFileParser {

    @Override
    public Map<String, Split> parse(String content) {

        Map<String, Split> splits = null;
        if (content == null) {
            return null;
        }
        try {
            Properties _properties = new Properties();
            _properties.load(new StringReader(content));
            splits = new HashMap<>();
            for (Object k: _properties.keySet()) {
                String splitName = (String) k;
                String treatment = _properties.getProperty((String) k);
                Split split = SplitHelper.createDefaultSplit(splitName);
                split.conditions = new ArrayList<>();
                split.conditions.add(SplitHelper.createRolloutCondition(treatment));
                splits.put(split.name, split);
            }
        } catch (Exception e){
            Logger.e("Error loading localhost property file");
        }
        return splits;
    }
}