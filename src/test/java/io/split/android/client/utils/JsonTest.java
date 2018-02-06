package io.split.android.client.utils;

import io.split.android.client.dtos.Partition;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class JsonTest {
    @Test
    public void unknownFieldsDontCauseProblems() {

        String json = "{\"treatment\":\"on\", \"size\":20, \"foo\":\"bar\"}";
        Partition partition = Json.fromJson(json, Partition.class);

        assertThat(partition.treatment, is(equalTo("on")));
        assertThat(partition.size, is(equalTo(20)));
    }
}
