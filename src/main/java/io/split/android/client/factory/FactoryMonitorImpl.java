package io.split.android.client.factory;

import android.support.annotation.VisibleForTesting;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import io.split.android.client.SplitFactory;

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
            totalCount += count.intValue();
        }
        return totalCount;
    }

    @Override
    public synchronized int count(String apiKey) {
        Integer count = factories.get(apiKey);
        return (count != null ? count.intValue() : 0);
    }

    @Override
    public synchronized void add(String apiKey) {
        Integer count = factories.get(apiKey);
        int newCount = (count != null ? count.intValue() : 0) + 1;
        factories.put(apiKey, new Integer(newCount));
    }

    @Override
    public synchronized void remove(String apiKey) {
        Integer count = factories.get(apiKey);
        int newCount = (count != null ? count.intValue() : 0) - 1;
        if(newCount > 0) {
            factories.put(apiKey, new Integer(newCount));
        } else {
            factories.remove(apiKey);
        }
    }
}
