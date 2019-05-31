import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitClientConfig;
import io.split.android.client.SplitFactory;
import io.split.android.client.SplitFactoryBuilder;
import io.split.android.client.api.Key;

public class FactoryTest {

    public void testDataFolderCreation() {

        String apiKey = "THE_API_KEY";
        SplitClient client;
        Key key = new Key("CUSTOMER_ID",null);
        SplitClientConfig config = SplitClientConfig.builder()
                //.endpoint(mAppConfig.getSdkUrl(), mAppConfig.getEventsUrl())
                .ready(30000)
                .featuresRefreshRate(30)
                .segmentsRefreshRate(30)
                .impressionsRefreshRate(30)
                .enableDebug()
                .trafficType("account")
                .eventsPerPush(2)
                //.impressionListener(this)
                .build();

        try {


            SplitFactory splitFactory = SplitFactoryBuilder.build(apiKey, key, config, getActivity().getApplicationContext());

            client = splitFactory.client();

        } catch (IOException | InterruptedException | URISyntaxException | TimeoutException e) {
            e.printStackTrace();
        }
    }
    }
}
