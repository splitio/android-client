package io.split.android.client.Localhost;

import com.google.common.base.Strings;

public class LocalhostGrammar {

    private static final String SPLIT_KEY_SEPARATOR = ":";

    public String buildSplitKeyName(String splitName, String matchingKey) {
        String newName;
        if(Strings.isNullOrEmpty(splitName) ) {
            return null;
        }

        if(!Strings.isNullOrEmpty(matchingKey)) {
            newName = splitName + SPLIT_KEY_SEPARATOR + matchingKey;
        } else {
            newName = splitName;
        }
        return newName;
    }

    public String getSplitName(String splitKeyName) {
        if(Strings.isNullOrEmpty(splitKeyName)) {
            return null;
        }
        String[] components = splitKeyName.split(SPLIT_KEY_SEPARATOR);
        return components[0];
    }

}
