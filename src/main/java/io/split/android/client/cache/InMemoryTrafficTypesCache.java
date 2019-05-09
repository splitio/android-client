package io.split.android.client.cache;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.Status;

public class InMemoryTrafficTypesCache implements ITrafficTypesCache {

    private Map<String, Integer> trafficTypes = null;

    public InMemoryTrafficTypesCache() {
        trafficTypes = Collections.synchronizedMap(new HashMap<String, Integer>());
    }

    @Override
    synchronized public void updateFromSplits(List<Split> splits) {
        if(splits != null) {
            for (Split split : splits) {
                if(split.status != null && split.trafficTypeName != null) {
                    if (split.status.equals(Status.ACTIVE)) {
                        add(split.trafficTypeName.toLowerCase());
                    } else {
                        remove(split.trafficTypeName.toLowerCase());
                    }
                }
            }
        }
    }

    @Override
    synchronized public void updateFromSplit(Split split) {
        if(split != null) {
            if(split.status != null && split.trafficTypeName != null) {
                if (split.status.equals(Status.ACTIVE)) {
                    add(split.trafficTypeName.toLowerCase());
                } else {
                    remove(split.trafficTypeName.toLowerCase());
                }
            }
        }
    }

    @Override
    public boolean contains(String name) {
        if(name == null) {
            return false;
        }
        return (trafficTypes.get(name.toLowerCase()) != null);
    }

    private void add(@NotNull String name) {
        int count = countFor(name);
        trafficTypes.put(name.toLowerCase(), Integer.valueOf(count++));
    }

    private void remove(@NotNull String name) {
        int count = countFor(name);
        if(count > 0) {
            trafficTypes.put(name, Integer.valueOf(count--));
        } else {
            trafficTypes.remove(name);
        }
    }

    private int countFor(@NotNull String name) {
        int count = 0;
        Integer countValue = trafficTypes.get(name);
        if(countValue != null) {
            count = countValue.intValue();
        }
        return count;
    }
}
