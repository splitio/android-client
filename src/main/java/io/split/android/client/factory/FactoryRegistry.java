package io.split.android.client.factory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FactoryRegistry {
    private Map<String, List<WeakFactory>> weakFactories;

    public FactoryRegistry() {
        weakFactories = Collections.synchronizedMap(new HashMap<String, List<WeakFactory>>());
    }

    public synchronized int count(String key) {
        int count = 0;
        compactFor(key);
        List<WeakFactory> factories = weakFactories.get(key);
        return (factories != null ? factories.size() : 0);
    }

    public synchronized int count() {
        int count = 0;
        for(String key : weakFactories.keySet()){
            count += count(key);
        }
        return count;
    }

    public synchronized void append(WeakFactory factory, String key) {
        List<WeakFactory> factories = weakFactories.get(key);
        if(factories == null) {
            factories = new ArrayList<>();
        }
        factories.add(factory);
    }

    private void compactFor(String key) {
        List<WeakFactory> refs = weakFactories.get(key);
        if(refs != null) {
            List<WeakFactory> filteredList = new ArrayList<>();
            for(WeakFactory weakFactory : refs) {
                if(weakFactory.getFactory() != null) {
                    filteredList.add(weakFactory);
                }
            }
            weakFactories.put(key, filteredList);
        }
    }
}
