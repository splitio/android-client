package io.split.android.client.factory;

import android.support.annotation.VisibleForTesting;

import java.util.HashMap;
import java.util.Map;

public class FactoryMonitorImpl implements FactoryMonitor {

    private Map<String, Integer> factories;

    private static FactoryMonitor sharedInstance;

    @VisibleForTesting
    public FactoryMonitorImpl() {
        factories = new HashMap<>();
    }

    public static FactoryMonitor getSharedInstance() {
        if(sharedInstance == null) {
            sharedInstance = new FactoryMonitorImpl();
        }
        return sharedInstance;
    }

    @Override
    public synchronized int count() {
        int totalCount = 0;
        for(Integer count : factories.values()) {
            totalCount += count;
        }
        return totalCount;
    }

    @Override
    public synchronized int count(String apiKey) {
        Integer count = factories.get(apiKey);
        return (count != null ? count : 0);
    }

    @Override
    public synchronized void add(String apiKey) {
        Integer count = factories.get(apiKey);
        int newCount = (count != null ? count : 0) + 1;
        factories.put(apiKey, newCount);
    }

    @Override
    public synchronized void remove(String apiKey) {
        Integer count = factories.get(apiKey);
        int newCount = (count != null ? count : 0) - 1;
        if(newCount > 0) {
            factories.put(apiKey, newCount);
        } else {
            factories.remove(apiKey);
        }
    }
}
