package io.split.android.client;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.Map;

import io.split.android.client.attributes.AttributesManager;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;

public interface SplitClient extends AttributesManager {

    /**
     * Returns the treatment to show this key for this feature flag. The set of treatments
     * for a feature flag can be configured on the Split user interface.
     * <p/>
     * <p/>
     * This method returns the string 'control' if:
     * <ol>
     * <li>Any of the parameters were null</li>
     * <li>There was an exception in evaluating the treatment</li>
     * <li>The SDK does not know of the existence of this feature flag</li>
     * <li>The feature flag was deleted through the Split user interface.</li>
     * </ol>
     * 'control' is a reserved treatment (you cannot create a treatment with the
     * same name) to highlight these exceptional circumstances.
     * <p/>
     * <p/>
     * The sdk returns the default treatment of this feature flag if:
     * <ol>
     * <li>The feature flag was killed</li>
     * <li>The key did not match any of the conditions in the feature roll-out plan</li>
     * </ol>
     * The default treatment of a feature flag is set on the Split user interface.
     * <p/>
     * <p/>
     * This method does not throw any exceptions. It also never returns null.
     *
     * @param featureFlagName the feature flag we want to evaluate. MUST NOT be null.
     * @return the evaluated treatment, the default treatment of this feature flag, or 'control'.
     */
    String getTreatment(String featureFlagName);

    /**
     * This method is useful when you want to determine the treatment to show
     * to an customer (user, account etc.) based on an attribute of that customer
     * instead of it's key.
     * <p/>
     * <p/>
     * Examples include showing a different treatment to users on trial plan
     * vs. premium plan. Another example is to show a different treatment
     * to users created after a certain date.
     *
     * @param featureFlagName the feature flag we want to evaluate. MUST NOT be null.
     * @param attributes      of the customer (user, account etc.) to use in evaluation. Can be null or empty.
     * @return the evaluated treatment, the default treatment of this feature flag, or 'control'.
     */
    String getTreatment(String featureFlagName, Map<String, Object> attributes);


    /**
     * This method is useful when you want to determine the treatment to show
     * to an customer (user, account etc.) based on an attribute of that customer
     * instead of it's key.
     * <p/>
     * <p/>
     * Examples include showing a different treatment to users on trial plan
     * vs. premium plan. Another example is to show a different treatment
     * to users created after a certain date.
     *
     * @param featureFlagName the feature flag we want to evaluate. MUST NOT be null.
     * @param attributes      of the customer (user, account etc.) to use in evaluation. Can be null or empty.
     * @return the evaluated treatment, the default treatment of this feature flag, or 'control'
     * with its corresponding configurations if it has one.
     */
    SplitResult getTreatmentWithConfig(String featureFlagName, Map<String, Object> attributes);

    /**
     * This method is useful when you want to determine the treatment of several feature flags at
     * the same time.
     * <p/>
     * <p/>
     * It can be used to cache treatments you know it won't change very often.
     *
     * @param featureFlagNames the feature flags you want to evaluate. MUST NOT be null.
     * @param attributes       of the customer (user, account etc.) to use in evaluation. Can be null or empty.
     * @return the evaluated treatments, the default treatment of a feature, or 'control'.
     */
    Map<String, String> getTreatments(List<String> featureFlagNames, Map<String, Object> attributes);


    /**
     * This method is useful when you want to determine the treatment of several feature flags at
     * the same time.
     * <p/>
     * <p/>
     * It can be used to cache treatments you know it won't change very often.
     *
     * @param featureFlagNames the feature flags you want to evaluate. MUST NOT be null.
     * @param attributes       of the customer (user, account etc.) to use in evaluation. Can be null or empty.
     * @return the evaluated treatments, the default treatment of a feature flag, or 'control'
     * with its corresponding configurations if it has one.
     */
    Map<String, SplitResult> getTreatmentsWithConfig(List<String> featureFlagNames, Map<String, Object> attributes);

    /**
     * This method is useful when you want to determine the treatment of several feature flags
     * belonging to a specific Flag Set at the same time.
     *
     * @param flagSet    the Flag Set name that you want to evaluate. Must not be null or empty
     * @param attributes of the customer (user, account etc.) to use in evaluation. Can be null or empty
     * @return a {@link Map} containing for each feature flag the evaluated treatment, the default treatment of this feature flag, or 'control'
     */
    Map<String, String> getTreatmentsByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes);

    /**
     * This method is useful when you want to determine the treatment of several feature flags
     * belonging to a specific list of Flag Sets at the same time.
     *
     * @param flagSets   the Flag Sets names that you want to evaluate. Must not be null or empty
     * @param attributes of the customer (user, account etc.) to use in evaluation. Can be null or empty
     * @return a {@link Map} containing for each feature flag the evaluated treatment, the default treatment of this feature flag, or 'control'
     */
    Map<String, String> getTreatmentsByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes);

    /**
     * This method is useful when you want to determine the treatment of several feature flags
     * belonging to a specific Flag Set
     *
     * @param flagSet    the Flag Set name that you want to evaluate. Must not be null or empty
     * @param attributes of the customer (user, account etc.) to use in evaluation. Can be null or empty
     * @return a {@link Map} containing for each feature flag the evaluated treatment, the default treatment of this feature flag, or 'control'
     */
    Map<String, SplitResult> getTreatmentsWithConfigByFlagSet(@NonNull String flagSet, @Nullable Map<String, Object> attributes);

    /**
     * This method is useful when you want to determine the treatment of several feature flags
     * belonging to a specific list of Flag Sets
     *
     * @param flagSets   the Flag Sets names that you want to evaluate. Must not be null or empty
     * @param attributes of the customer (user, account etc.) to use in evaluation. Can be null or empty
     * @return a {@link Map} containing for each feature flag the evaluated treatment, the default treatment of this feature flag, or 'control'
     */
    Map<String, SplitResult> getTreatmentsWithConfigByFlagSets(@NonNull List<String> flagSets, @Nullable Map<String, Object> attributes);

    /**
     * Destroys the background processes and clears the cache, releasing the resources used by
     * any instances of SplitClient or SplitManager generated by the client's parent SplitFactory
     */
    void destroy();

    /**
     * Flushes all memory allocated queues which should be stored on disk or sent to server.
     */
    void flush();

    /**
     * Checks if cached data is ready to perform treatment evaluations
     *
     * @return true if the sdk is ready, if false, calls to getTreatment will return control
     */
    boolean isReady();

    void on(SplitEvent event, SplitEventTask task);

    /**
     * Enqueue a new event to be sent to Split data collection services.
     * <p>
     * The traffic type used is the one set by trafficType() in SplitClientConfig.
     * <p>
     * Example:
     * client.track(“checkout”)
     *
     * @param eventType the type of the event
     * @return true if the track was successful, false otherwise
     */
    boolean track(String eventType);

    /**
     * Enqueue a new event to be sent to Split data collection services
     * <p>
     * Example:
     * client.track(“account”, “checkout”, 200.00)
     *
     * @param trafficType the type of the event
     * @param eventType   the type of the event
     * @param value       the value of the event
     * @return true if the track was successful, false otherwise
     */
    boolean track(String trafficType, String eventType, double value);

    /**
     * Enqueue a new event to be sent to Split data collection services
     * <p>
     * Example:
     * client.track(“account”, “checkout”)
     *
     * @param trafficType the type of the event
     * @param eventType   the type of the event
     * @return true if the track was successful, false otherwise
     */
    boolean track(String trafficType, String eventType);

    /**
     * Enqueue a new event to be sent to Split data collection services
     * <p>
     * The traffic type used is the one set by trafficType() in SplitClientConfig.
     * <p>
     * Example:
     * client.track(“checkout”, 200.00)
     *
     * @param eventType the type of the event
     * @param value     the value of the event
     * @return true if the track was successful, false otherwise
     */
    boolean track(String eventType, double value);

    /**
     * Enqueue a new event to be sent to Split data collection services.
     * <p>
     * The traffic type used is the one set by trafficType() in SplitClientConfig.
     * <p>
     * Example:
     * client.track(“checkout”)
     *
     * @param eventType  the type of the event
     * @param properties custom user data map
     * @return true if the track was successful, false otherwise
     */
    boolean track(String eventType, Map<String, Object> properties);

    /**
     * Enqueue a new event to be sent to Split data collection services
     * <p>
     * Example:
     * client.track(“account”, “checkout”, 200.00)
     *
     * @param trafficType the type of the event
     * @param eventType   the type of the event
     * @param value       the value of the event
     * @param properties  custom user data map
     * @return true if the track was successful, false otherwise
     */
    boolean track(String trafficType, String eventType, double value, Map<String, Object> properties);

    /**
     * Enqueue a new event to be sent to split data collection services
     * <p>
     * Example:
     * client.track(“account”, “checkout”)
     *
     * @param trafficType the type of the event
     * @param eventType   the type of the event
     * @param properties  custom user data map
     * @return true if the track was successful, false otherwise
     */
    boolean track(String trafficType, String eventType, Map<String, Object> properties);

    /**
     * Enqueue a new event to be sent to Split data collection services
     * <p>
     * The traffic type used is the one set by trafficType() in SplitClientConfig.
     * <p>
     * Example:
     * client.track(“checkout”, 200.00)
     *
     * @param eventType  the type of the event
     * @param value      the value of the event
     * @param properties custom user data map
     * @return true if the track was successful, false otherwise
     */
    boolean track(String eventType, double value, Map<String, Object> properties);
}
