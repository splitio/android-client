package io.split.android.client.utils;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

    public String join(String connector, Iterable<String> values) {
        if(values == null || connector == null) {
            return "";
        }

        Iterator iterator = values.iterator();
        if(!iterator.hasNext()) {
            return "";
        }

        StringBuilder string = new StringBuilder(iterator.next().toString());
        while (iterator.hasNext()) {
            string.append(connector).append(iterator.next().toString());
        }

        return string.toString();
    }
}
