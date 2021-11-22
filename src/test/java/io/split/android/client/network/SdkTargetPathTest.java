package io.split.android.client.network;

import static org.junit.Assert.*;

import org.junit.Test;

import java.net.URI;
import java.net.URISyntaxException;

public class SdkTargetPathTest {

    @Test
    public void userKeyWithSpaces() throws URISyntaxException {
        URI uri = SdkTargetPath.mySegments("https://split.io", "CABM, CCIB Marketing");

        assertEquals("/mySegments/CABM,%20CCIB%20Marketing", uri.getRawPath());
    }

    @Test
    public void userKeyWithSlash() throws URISyntaxException {
        URI uri = SdkTargetPath.mySegments("https://split.io", "user/key");

        assertEquals("/mySegments/user%2Fkey", uri.getRawPath());
    }

    @Test
    public void userKeyWithSpecialChars() throws URISyntaxException {
        URI uri = SdkTargetPath.mySegments("https://split.io", "grüneStraße");

        assertEquals("/mySegments/gr%C3%BCneStra%C3%9Fe", uri.getRawPath());
    }

}