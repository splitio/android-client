package io.split.android.client.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import io.split.android.client.dtos.Partition;

public class JsonTest {

    @Test
    public void unknownFieldsDontCauseProblems() {

        String json = "{\"treatment\":\"on\", \"size\":20, \"foo\":\"bar\"}";
        Partition partition = Json.fromJson(json, Partition.class);

        assertEquals("on", partition.treatment);
        assertEquals(20, partition.size);
    }
}
