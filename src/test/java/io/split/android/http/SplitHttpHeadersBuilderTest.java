package io.split.android.http;

import org.junit.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import io.split.android.client.network.SplitHttpHeadersBuilder;

public class SplitHttpHeadersBuilderTest {

    final String API_KEY = "99049fd8653247c5ea42bc3c1ae2c6a42bc3";
    final String VERSION = "Android-2.4.0";
    final String HOST_NAME = "hostname";
    final String HOST_IP = "192.168.1.2";

    final String CLIENT_MACHINE_NAME_HEADER = "SplitSDKMachineName";
    final String CLIENT_MACHINE_IP_HEADER = "SplitSDKMachineIP";
    final String CLIENT_VERSION = "SplitSDKVersion";
    final String AUTHORIZATION = "Authorization";

    @Before
    public void setup() {
    }

    @Test
    public void allHeaders() {
        SplitHttpHeadersBuilder builder = new SplitHttpHeadersBuilder();
        builder.setApiToken(API_KEY)
                .setClientVersion(VERSION)
                .setHostname(HOST_NAME)
                .setHostIp(HOST_IP);

        Map<String, String> headers = builder.build();


        Assert.assertEquals("Bearer " + API_KEY, headers.get(AUTHORIZATION));
        Assert.assertEquals(VERSION, headers.get(CLIENT_VERSION));
        Assert.assertEquals(HOST_NAME, headers.get(CLIENT_MACHINE_NAME_HEADER));
        Assert.assertEquals(HOST_IP, headers.get(CLIENT_MACHINE_IP_HEADER));
    }

    @Test
    public void missingAuthorization() {
        boolean validated = false;
        SplitHttpHeadersBuilder builder = new SplitHttpHeadersBuilder();
        builder.setClientVersion(VERSION)
                .setHostname(HOST_NAME)
                .setHostIp(HOST_IP);

        try {
            Map<String, String> headers = builder.build();
        } catch (IllegalArgumentException e) {
            validated = true;
        }

        Assert.assertTrue(validated);
    }

    @Test
    public void missingClientVersion() {
        boolean validated = false;
        SplitHttpHeadersBuilder builder = new SplitHttpHeadersBuilder();
        builder.setApiToken(API_KEY)
                .setHostname(HOST_NAME)
                .setHostIp(HOST_IP);

        try {
            Map<String, String> headers = builder.build();
        } catch (IllegalArgumentException e) {
            validated = true;
        }

        Assert.assertTrue(validated);
    }

    @Test
    public void allowedEmptyHeaders() {
        SplitHttpHeadersBuilder builder = new SplitHttpHeadersBuilder();
        builder.setApiToken(API_KEY)
                .setClientVersion(VERSION);

        Map<String, String> headers = builder.build();


        Assert.assertEquals("Bearer " + API_KEY, headers.get(AUTHORIZATION));
        Assert.assertEquals(VERSION, headers.get(CLIENT_VERSION));
        Assert.assertNull(headers.get(CLIENT_MACHINE_NAME_HEADER));
        Assert.assertNull(headers.get(CLIENT_MACHINE_IP_HEADER));
    }

    @After
    public void teardown() {
    }
}
