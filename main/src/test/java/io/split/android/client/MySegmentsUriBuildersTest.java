package io.split.android.client;

import static org.mockito.Mockito.mockStatic;

import org.junit.Test;
import org.mockito.MockedStatic;

import java.net.URISyntaxException;

import io.split.android.client.network.SdkTargetPath;

public class MySegmentsUriBuildersTest {

    @Test
    public void mySegmentsUriBuilderUsesSdkTargetPath() throws URISyntaxException {
        try (MockedStatic<SdkTargetPath> mockedStatic = mockStatic(SdkTargetPath.class)) {

            SplitFactoryHelper.MySegmentsUriBuilder builder = new SplitFactoryHelper.MySegmentsUriBuilder(
                    "https://sdk.split.io/api/");
            builder.build("some_key");

            mockedStatic.verify(() -> SdkTargetPath.mySegments("https://sdk.split.io/api/", "some_key"));
        }
    }
}
