package service;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import io.split.android.client.service.sseclient.SseChannelsParser;

public class SseChannelsParserTest {

    @Before
    public void setup()  {
    }

    @Test
    public void testOkToken() {
        String jwtToken = "eyJhbGciOiJIUzI1NiIsImtpZCI6ImtleUlkIiwidHlwIjoiSldUIn0.eyJvcmdJ" +
                "ZCI6ImY3ZjAzNTIwLTVkZjctMTFlOC04NDc2LTBlYzU0NzFhM2NlYyIsImVudklkIjoiZjdmN" +
                "jI4OTAtNWRmNy0xMWU4LTg0NzYtMGVjNTQ3MWEzY2VjIiwidXNlcktleXMiOlsiamF2aSJdLC" +
                "J4LWFibHktY2FwYWJpbGl0eSI6IntcIk16TTVOamMwT0RjeU5nPT1fTVRFeE16Z3dOamd4X01" +
                "UY3dOVEkyTVRNME1nPT1fbXlTZWdtZW50c1wiOltcInN1YnNjcmliZVwiXSxcIk16TTVOamMwT" +
                "0RjeU5nPT1fTVRFeE16Z3dOamd4X3NwbGl0c1wiOltcInN1YnNjcmliZVwiXSxcImNvbnRyb2x" +
                "cIjpbXCJzdWJzY3JpYmVcIl19IiwieC1hYmx5LWNsaWVudElkIjoiY2xpZW50SWQiLCJleHAiOj" +
                "E1ODM5NDc4MTIsImlhdCI6MTU4Mzk0NDIxMn0.bSkxugrXKLaJJkvlND1QEd7vrwqWiPjn77pkrJOl4t8";

        SseChannelsParser parser = new SseChannelsParser();
        List<String> channels = parser.parse(jwtToken);

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

        SseChannelsParser parser = new SseChannelsParser();
        List<String> channels = parser.parse(jwtToken);

        Assert.assertEquals(0, channels.size());
    }

    @Test
    public void testOnlyChannelsWithSeparator() {
        String jwtToken = ".eyJvcmdJ" +
                "ZCI6ImY3ZjAzNTIwLTVkZjctMTFlOC04NDc2LTBlYzU0NzFhM2NlYyIsImVudklkIjoiZjdmN" +
                "jI4OTAtNWRmNy0xMWU4LTg0NzYtMGVjNTQ3MWEzY2VjIiwidXNlcktleXMiOlsiamF2aSJdLC" +
                "J4LWFibHktY2FwYWJpbGl0eSI6IntcIk16TTVOamMwT0RjeU5nPT1fTVRFeE16Z3dOamd4X01" +
                "UY3dOVEkyTVRNME1nPT1fbXlTZWdtZW50c1wiOltcInN1YnNjcmliZVwiXSxcIk16TTVOamMwT" +
                "0RjeU5nPT1fTVRFeE16Z3dOamd4X3NwbGl0c1wiOltcInN1YnNjcmliZVwiXSxcImNvbnRyb2x" +
                "cIjpbXCJzdWJzY3JpYmVcIl19IiwieC1hYmx5LWNsaWVudElkIjoiY2xpZW50SWQiLCJleHAiOj" +
                "E1ODM5NDc4MTIsImlhdCI6MTU4Mzk0NDIxMn0.bSkxugrXKLaJJkvlND1QEd7vrwqWiPjn77pkrJOl4t8";

        SseChannelsParser parser = new SseChannelsParser();
        List<String> channels = parser.parse(jwtToken);

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

        SseChannelsParser parser = new SseChannelsParser();
        List<String> channels = parser.parse(jwtToken);

        Assert.assertEquals(0, channels.size());
    }

    @Test
    public void garbageToken() {
        String jwtToken = "novalidtoken";

        SseChannelsParser parser = new SseChannelsParser();
        List<String> channels = parser.parse(jwtToken);

        Assert.assertEquals(0, channels.size());
    }

    @Test
    public void emptyToken() {
        String jwtToken = "";

        SseChannelsParser parser = new SseChannelsParser();
        List<String> channels = parser.parse(jwtToken);

        Assert.assertEquals(0, channels.size());
    }

    @Test
    public void nullToken() {
        String jwtToken = "";

        SseChannelsParser parser = new SseChannelsParser();
        List<String> channels = parser.parse(jwtToken);

        Assert.assertEquals(0, channels.size());
    }

}
