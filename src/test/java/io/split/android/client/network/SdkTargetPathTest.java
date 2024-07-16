package io.split.android.client.network;

import static org.junit.Assert.*;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class SdkTargetPathTest {

    @Test
    public void userKeyWithSpaces() throws URISyntaxException {
        URI uri = SdkTargetPath.mySegments("https://split.io", "CABM, CCIB Marketing");
        URI uri2 = SdkTargetPath.myLargeSegments("https://split.io", "CABM, CCIB Marketing");

        assertEquals("/mySegments/CABM,%20CCIB%20Marketing", uri.getRawPath());
        assertEquals("/myLargeSegments/CABM,%20CCIB%20Marketing", uri2.getRawPath());
    }

    @Test
    public void userKeyWithSlash() throws URISyntaxException {
        URI uri = SdkTargetPath.mySegments("https://split.io", "user/key");
        URI uri2 = SdkTargetPath.myLargeSegments("https://split.io", "user/key");

        assertEquals("/mySegments/user%2Fkey", uri.getRawPath());
        assertEquals("/myLargeSegments/user%2Fkey", uri2.getRawPath());
    }

    @Test
    public void userKeyWithSpecialChars() throws URISyntaxException {
        URI uri = SdkTargetPath.mySegments("https://split.io", "grüneStraße");
        URI uri2 = SdkTargetPath.myLargeSegments("https://split.io", "grüneStraße");

        assertEquals("/mySegments/gr%C3%BCneStra%C3%9Fe", uri.getRawPath());
        assertEquals("/myLargeSegments/gr%C3%BCneStra%C3%9Fe", uri2.getRawPath());
    }

    @Test
    public void configTelemetryPathIsCorrect() throws URISyntaxException {
        URI uri = SdkTargetPath.telemetryConfig("https://split.io");

        assertEquals("/metrics/config", uri.getRawPath());
    }

    @Test
    public void statsTelemetryPathIsCorrect() throws URISyntaxException {
        URI uri = SdkTargetPath.telemetryStats("https://split.io");

        assertEquals("/metrics/usage", uri.getRawPath());
    }

    @Test
    public void uniqueKeysPathIsCorrect() throws URISyntaxException {
        URI uri = SdkTargetPath.uniqueKeys("https://split.io");

        assertEquals("/keys/cs", uri.getRawPath());
    }
}
