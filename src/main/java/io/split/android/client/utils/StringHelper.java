package io.split.android.client.utils;

import java.util.List;

public class StringHelper {
    public String join(String connector, List<String> list) {
        if(list == null || list.size() == 0 || connector == null) {
            return "";
        }
        StringBuilder string = new StringBuilder(list.get(0));
        for(int i=1; i<list.size(); i++) {
            string.append(connector).append(list.get(i));
        }
        return string.toString();
    }
}
