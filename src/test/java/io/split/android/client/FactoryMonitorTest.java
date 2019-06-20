package io.split.android.client;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import io.split.android.client.factory.FactoryMonitor;
import io.split.android.client.factory.FactoryMonitorImpl;
import io.split.android.fake.SplitFactoryStub;

public class FactoryMonitorTest {

    FactoryMonitor monitor;

    @Before
    public void setup() {
        monitor = new FactoryMonitorImpl();
    }

    @Test
    public void testCountFactories() {

        monitor.add("k1");
        monitor.add("k1");
        monitor.add("k2");
        monitor.add("k3");
        monitor.add("k4");
        monitor.add("k4");
        monitor.add("k4");

        Assert.assertEquals(2, monitor.count("k1"));
        Assert.assertEquals(1, monitor.count("k2"));
        Assert.assertEquals(1, monitor.count("k3"));
        Assert.assertEquals(3, monitor.count("k4"));
        Assert.assertEquals(7, monitor.count());

    }


    @Test
    public void testCountWithRemovedFactories() {

        monitor.add("k1");
        monitor.add("k1");
        monitor.add("k2");
        monitor.add("k3");
        monitor.add("k4");
        monitor.add("k4");
        monitor.add("k4");

        int k1Count = monitor.count("k1");
        int k2Count = monitor.count("k2");
        int k4Count = monitor.count("k4");
        int allCount = monitor.count();

        monitor.remove("k1");
        monitor.remove("k2");
        monitor.remove("k4");

        Assert.assertEquals(2, k1Count);
        Assert.assertEquals(1, k2Count);
        Assert.assertEquals(3, k4Count);
        Assert.assertEquals(7, allCount);


        Assert.assertEquals(1, monitor.count("k1"));
        Assert.assertEquals(0, monitor.count("k2"));
        Assert.assertEquals(1, monitor.count("k3"));
        Assert.assertEquals(2, monitor.count("k4"));
        Assert.assertEquals(4, monitor.count());

    }

}

