package io.split.android.integration.service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.split.android.client.service.sseclient.InvalidJwtTokenException;
import io.split.android.client.service.sseclient.SseJwtParser;
import io.split.android.client.service.sseclient.SseJwtToken;


public class SseJwtTokenParserTest {
    // This class is tested as an instrumented test because of
    // the behavior of the Base64.decode funcion is not correct in unit test
    @Before
    public void setup() {
    }

    @Test
    public void testOkToken() throws InvalidJwtTokenException {
        String jwtToken = "eyJhbGciOiJIUzI1NiIsImtpZCI6ImtleUlkIiwidHlwIjoiSldUIn0.eyJvcmdJ" +
                "ZCI6ImY3ZjAzNTIwLTVkZjctMTFlOC04NDc2LTBlYzU0NzFhM2NlYyIsImVudklkIjoiZjdmN" +
                "jI4OTAtNWRmNy0xMWU4LTg0NzYtMGVjNTQ3MWEzY2VjIiwidXNlcktleXMiOlsiamF2aSJdLC" +
                "J4LWFibHktY2FwYWJpbGl0eSI6IntcIk16TTVOamMwT0RjeU5nPT1fTVRFeE16Z3dOamd4X01" +
                "UY3dOVEkyTVRNME1nPT1fbXlTZWdtZW50c1wiOltcInN1YnNjcmliZVwiXSxcIk16TTVOamMwT" +
                "0RjeU5nPT1fTVRFeE16Z3dOamd4X3NwbGl0c1wiOltcInN1YnNjcmliZVwiXSxcImNvbnRyb2x" +
                "cIjpbXCJzdWJzY3JpYmVcIl19IiwieC1hYmx5LWNsaWVudElkIjoiY2xpZW50SWQiLCJleHAiOj" +
                "E1ODM5NDc4MTIsImlhdCI6MTU4Mzk0NDIxMn0.bSkxugrXKLaJJkvlND1QEd7vrwqWiPjn77pkrJOl4t8";

        SseJwtParser parser = new SseJwtParser();
        SseJwtToken parsedToken = parser.parse(jwtToken);
        List<String> channels = parsedToken.getChannels();

        Assert.assertEquals(1583947812L, parsedToken.getExpirationTime());
        Assert.assertEquals(jwtToken, parsedToken.getRawJwt());
        Assert.assertEquals("MzM5Njc0ODcyNg==_MTExMzgwNjgx_MTcwNTI2MTM0Mg==_mySegments",
                channels.get(0));
        Assert.assertEquals("MzM5Njc0ODcyNg==_MTExMzgwNjgx_splits",
                channels.get(1));
        Assert.assertEquals("control",
                channels.get(2));
    }

    @Test
    public void onlyHeader() {
        String jwtToken = "eyJhbGciOiJIUzI1NiIsImtpZCI6ImtleUlkIiwidHlwIjoiSldUIn0.";

        SseJwtParser parser = new SseJwtParser();
        Exception thrownEx = null;

        SseJwtToken parsedToken = null;
        try {
            parsedToken = parser.parse(jwtToken);
        } catch (InvalidJwtTokenException e) {
            thrownEx = e;
        }

        Assert.assertNull(parsedToken);
        Assert.assertNotNull(thrownEx);
    }

    @Test
    public void testOnlyChannelsWithSeparator() throws InvalidJwtTokenException {
        String jwtToken = ".eyJvcmdJ" +
                "ZCI6ImY3ZjAzNTIwLTVkZjctMTFlOC04NDc2LTBlYzU0NzFhM2NlYyIsImVudklkIjoiZjdmN" +
                "jI4OTAtNWRmNy0xMWU4LTg0NzYtMGVjNTQ3MWEzY2VjIiwidXNlcktleXMiOlsiamF2aSJdLC" +
                "J4LWFibHktY2FwYWJpbGl0eSI6IntcIk16TTVOamMwT0RjeU5nPT1fTVRFeE16Z3dOamd4X01" +
                "UY3dOVEkyTVRNME1nPT1fbXlTZWdtZW50c1wiOltcInN1YnNjcmliZVwiXSxcIk16TTVOamMwT" +
                "0RjeU5nPT1fTVRFeE16Z3dOamd4X3NwbGl0c1wiOltcInN1YnNjcmliZVwiXSxcImNvbnRyb2x" +
                "cIjpbXCJzdWJzY3JpYmVcIl19IiwieC1hYmx5LWNsaWVudElkIjoiY2xpZW50SWQiLCJleHAiOj" +
                "E1ODM5NDc4MTIsImlhdCI6MTU4Mzk0NDIxMn0.bSkxugrXKLaJJkvlND1QEd7vrwqWiPjn77pkrJOl4t8";

        SseJwtParser parser = new SseJwtParser();
        SseJwtToken parsedToken = parser.parse(jwtToken);
        List<String> channels = parsedToken.getChannels();

        Assert.assertEquals(1583947812L, parsedToken.getExpirationTime());
        Assert.assertEquals(jwtToken, parsedToken.getRawJwt());
        Assert.assertEquals("MzM5Njc0ODcyNg==_MTExMzgwNjgx_MTcwNTI2MTM0Mg==_mySegments",
                channels.get(0));
        Assert.assertEquals("MzM5Njc0ODcyNg==_MTExMzgwNjgx_splits",
                channels.get(1));
        Assert.assertEquals("control",
                channels.get(2));
    }

    @Test
    public void testOnlyChannelsWithoutSeparator() {
        String jwtToken = "eyJvcmdJ" +
                "ZCI6ImY3ZjAzNTIwLTVkZjctMTFlOC04NDc2LTBlYzU0NzFhM2NlYyIsImVudklkIjoiZjdmN" +
                "jI4OTAtNWRmNy0xMWU4LTg0NzYtMGVjNTQ3MWEzY2VjIiwidXNlcktleXMiOlsiamF2aSJdLC" +
                "J4LWFibHktY2FwYWJpbGl0eSI6IntcIk16TTVOamMwT0RjeU5nPT1fTVRFeE16Z3dOamd4X01" +
                "UY3dOVEkyTVRNME1nPT1fbXlTZWdtZW50c1wiOltcInN1YnNjcmliZVwiXSxcIk16TTVOamMwT" +
                "0RjeU5nPT1fTVRFeE16Z3dOamd4X3NwbGl0c1wiOltcInN1YnNjcmliZVwiXSxcImNvbnRyb2x" +
                "cIjpbXCJzdWJzY3JpYmVcIl19IiwieC1hYmx5LWNsaWVudElkIjoiY2xpZW50SWQiLCJleHAiOj" +
                "E1ODM5NDc4MTIsImlhdCI6MTU4Mzk0NDIxMn0.bSkxugrXKLaJJkvlND1QEd7vrwqWiPjn77pkrJOl4t8";

        SseJwtParser parser = new SseJwtParser();
        Exception thrownEx = null;

        SseJwtToken parsedToken = null;
        try {
            parsedToken = parser.parse(jwtToken);
        } catch (InvalidJwtTokenException e) {
            thrownEx = e;
        }

        Assert.assertNull(parsedToken);
        Assert.assertNotNull(thrownEx);
    }

    @Test
    public void garbageToken() {
        String jwtToken = "novalidtoken";

        SseJwtParser parser = new SseJwtParser();
        Exception thrownEx = null;

        SseJwtToken parsedToken = null;
        try {
            parsedToken = parser.parse(jwtToken);
        } catch (InvalidJwtTokenException e) {
            thrownEx = e;
        }

        Assert.assertNull(parsedToken);
        Assert.assertNotNull(thrownEx);
    }

    @Test
    public void emptyToken() {
        String jwtToken = "";

        SseJwtParser parser = new SseJwtParser();
        Exception thrownEx = null;

        SseJwtToken parsedToken = null;
        try {
            parsedToken = parser.parse(jwtToken);
        } catch (InvalidJwtTokenException e) {
            thrownEx = e;
        }

        Assert.assertNull(parsedToken);
        Assert.assertNotNull(thrownEx);
    }

    @Test
    public void nullToken() {
        String jwtToken = "";

        SseJwtParser parser = new SseJwtParser();
        Exception thrownEx = null;

        SseJwtToken parsedToken = null;
        try {
            parsedToken = parser.parse(jwtToken);
        } catch (InvalidJwtTokenException e) {
            thrownEx = e;
        }

        Assert.assertNull(parsedToken);
        Assert.assertNotNull(thrownEx);
    }

    @Test
    public void testLargeSegmentsToken() throws InvalidJwtTokenException {
        String jwtToken = "eyJhbGciOiJIUzI1NiIsImtpZCI6IjVZOU05US45QnJtR0EiLCJ0eXAiOiJKV1QifQ." +
                "ewogICJ4LWFibHktY2FwYWJpbGl0eSI6ICJ7XCJNek01TmpjME9EY3lOZz09X01URXhNemd3Tm" +
                "pneF9NVGN3TlRJMk1UTTBNZz09X215U2VnbWVudHNcIjpbXCJzdWJzY3JpYmVcIl0sXCJNek01" +
                "TmpjME9EY3lOZz09X01URXhNemd3TmpneF9NVGN3TlRJMk1UTTBNZz09X215bGFyZ2VzZWdtZW" +
                "50c1wiOltcInN1YnNjcmliZVwiXSxcIk16TTVOamMwT0RjeU5nPT1fTVRFeE16Z3dOamd4X3Nw" +
                "bGl0c1wiOltcInN1YnNjcmliZVwiXSxcImNvbnRyb2xfcHJpXCI6W1wic3Vic2NyaWJlXCIsXC" +
                "JjaGFubmVsLW1ldGFkYXRhOnB1Ymxpc2hlcnNcIl0sXCJjb250cm9sX3NlY1wiOltcInN1YnNj" +
                "cmliZVwiLFwiY2hhbm5lbC1tZXRhZGF0YTpwdWJsaXNoZXJzXCJdfSIsCiAgIngtYWJseS1jbG" +
                "llbnRJZCI6ICJjbGllbnRJZCIsCiAgImV4cCI6IDIyMDg5ODg4MDAsCiAgImlhdCI6IDE1ODc0M" +
                "DQzODgKfQ==.LcKAXnkr-CiYVxZ7l38w9i98Y-BMAv9JlGP2i92nVQY";

        SseJwtParser parser = new SseJwtParser();
        SseJwtToken parsedToken = parser.parse(jwtToken);
        List<String> channels = parsedToken.getChannels();

        Assert.assertEquals(2208988800L, parsedToken.getExpirationTime());
        Assert.assertEquals(jwtToken, parsedToken.getRawJwt());
        Assert.assertEquals("MzM5Njc0ODcyNg==_MTExMzgwNjgx_MTcwNTI2MTM0Mg==_mySegments",
                channels.get(0));
        Assert.assertEquals("MzM5Njc0ODcyNg==_MTExMzgwNjgx_MTcwNTI2MTM0Mg==_mylargesegments",
                channels.get(1));
        Assert.assertEquals("MzM5Njc0ODcyNg==_MTExMzgwNjgx_splits",
                channels.get(2));
        Assert.assertEquals("[?occupancy=metrics.publishers]control_pri",
                channels.get(3));
        Assert.assertEquals("[?occupancy=metrics.publishers]control_sec",
                channels.get(4));
    }
}
