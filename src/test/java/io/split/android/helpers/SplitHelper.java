package io.split.android.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitHelper {
    public static Map<String, String> createConfigs(List<String> treatments, List<String> configs) {
        Map<String, String> config = new HashMap<>();
        int i = 0;
        while(treatments.size() < i && config.size() < i){
            config.put(treatments.get(i), configs.get(i));
            i++;
        }
        return config;
    }
}
