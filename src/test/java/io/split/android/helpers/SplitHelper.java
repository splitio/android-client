package io.split.android.helpers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitHelper {
    public static Map<String, String> createConfigs(List<String> treatments, List<String> configs) {
        Map<String, String> config = new HashMap<>();
        int i = 0;
        for(String treatment : treatments) {
            if(i > config.size() - 1) {
                return config;
            }
            config.put(treatment, configs.get(i));
            i++;
        }
        return config;
    }
}
