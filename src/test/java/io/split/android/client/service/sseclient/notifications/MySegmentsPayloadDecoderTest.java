package io.split.android.client.service.sseclient.notifications;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class MySegmentsPayloadDecoderTest {

    private MySegmentsPayloadDecoder mMySegmentsPayloadDecoder;

    @Before
    public void setUp() {
        mMySegmentsPayloadDecoder = new MySegmentsPayloadDecoder();
    }

    @Test
    public void encodingOfUserKeyWorksAsExpected() {
        String expectedResult = "MjAwNjI0Nzg3NQ==";

        assertEquals(expectedResult, mMySegmentsPayloadDecoder.hashUserKeyForMySegmentsV1("user_key"));
    }
}
