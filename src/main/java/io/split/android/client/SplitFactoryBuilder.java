package io.split.android.client;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.concurrent.TimeoutException;

import io.split.android.client.api.Key;
import io.split.android.grammar.Treatments;
import timber.log.Timber;

/**
 * Builds an instance of SplitClient.
 */
public class SplitFactoryBuilder {

    /**
     *
     * @param apiToken
     * @param matchingkey
     * @param context
     * @return
     * @throws IOException
     * @throws InterruptedException
     * @throws TimeoutException
     * @throws URISyntaxException
     */
    public static SplitFactory build(String apiToken, String matchingkey, Context context) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
        Key key = new Key(matchingkey, null);
        return build(apiToken, key, context);
    }

    /**
     * Instantiates a SplitFactory with default configurations
     *
     * @param apiToken the API token. MUST NOT be null
     * @return a SplitFactory
     * @throws IOException                           if the SDK was being started in 'localhost' mode, but
     *                                               there were problems reading the override file from disk.
     * @throws java.lang.InterruptedException        if you asked to block until the sdk was
     *                                               ready and the block was interrupted.
     * @throws java.util.concurrent.TimeoutException if you asked to block until the sdk was
     *                                               ready and the timeout specified via config#ready() passed.
     */
    public static SplitFactory build(String apiToken, Key key, Context context) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
        return build(apiToken, key, SplitClientConfig.builder().build(), context);
    }

    /**
     * @param apiToken the API token. MUST NOT be null
     * @param config   parameters to control sdk construction. MUST NOT be null.
     * @return a SplitFactory
     * @throws java.io.IOException                   if the SDK was being started in 'localhost' mode, but
     *                                               there were problems reading the override file from disk.
     * @throws InterruptedException                  if you asked to block until the sdk was
     *                                               ready and the block was interrupted.
     * @throws java.util.concurrent.TimeoutException if you asked to block until the sdk was
     *                                               ready and the timeout specified via config#ready() passed.
     */
    public static synchronized SplitFactory build(String apiToken, Key key, SplitClientConfig config, Context context) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
        if (LocalhostSplitFactory.LOCALHOST.equals(apiToken)) {
            return LocalhostSplitFactory.createLocalhostSplitFactory(key.matchingKey());
        } else {
            return new SplitFactoryImpl(apiToken, key, config, context);

        }
    }

    /**
     * Instantiates a local Off-The-Grid SplitFactory
     *
     * @return a SplitFactory
     * @throws IOException if there were problems reading the override file from disk.
     */
    public static SplitFactory local(String key) throws IOException {
        return LocalhostSplitFactory.createLocalhostSplitFactory(key);
    }

    /**
     * Instantiates a local Off-The-Grid SplitFactory
     *
     * @param home A directory containing the .split file from which to build treatments. MUST NOT be null
     * @return a SplitFactory
     * @throws IOException if there were problems reading the override file from disk.
     */
    public static SplitFactory local(String home, String key) throws IOException {
        return new LocalhostSplitFactory(home, key);
    }

    public static void main(String... args) throws IOException, InterruptedException, TimeoutException, URISyntaxException {
        if (args.length != 1) {
            System.out.println("Usage: <api_token>");
            System.exit(1);
            return;
        }

        SplitClientConfig config = SplitClientConfig.builder().build();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if ("exit".equals(line)) {
                    System.exit(0);
                }
                String[] userIdAndSplit = line.split(" ");

                if (userIdAndSplit.length != 2) {
                    System.out.println("Could not understand command");
                    continue;
                }
                Key k = new Key(userIdAndSplit[0], null);
                SplitClient client = SplitFactoryBuilder.build("API_KEY", k, config, null).client();

                boolean isOn = client.getTreatment(userIdAndSplit[1]).equals("on");

                System.out.println(isOn ? Treatments.ON : Treatments.OFF);
            }
        } catch (IOException io) {
            Timber.e(io);
        }
    }
}
