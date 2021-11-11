package fake;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.split.android.client.SplitClient;
import io.split.android.client.SplitResult;
import io.split.android.client.events.SplitEvent;
import io.split.android.client.events.SplitEventTask;

public class SplitClientStub implements SplitClient {
    @Override
    public String getTreatment(String split) {
        return getTreatment(split);
    }

    @Override
    public String getTreatment(String split, Map<String, Object> attributes) {
        return "control";
    }

    @Override
    public SplitResult getTreatmentWithConfig(String split, Map<String, Object> attributes) {
        return null;
    }

    @Override
    public Map<String, String> getTreatments(List<String> splits, Map<String, Object> attributes) {
        return null;
    }

    @Override
    public Map<String, SplitResult> getTreatmentsWithConfig(List<String> splits, Map<String, Object> attributes) {
        return null;
    }

    @Override
    public void destroy() {

    }

    @Override
    public void flush() {

    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void on(SplitEvent event, SplitEventTask task) {

    }

    @Override
    public boolean track(String eventType) {
        return false;
    }

    @Override
    public boolean track(String trafficType, String eventType, double value) {
        return false;
    }

    @Override
    public boolean track(String trafficType, String eventType) {
        return false;
    }

    @Override
    public boolean track(String eventType, double value) {
        return false;
    }

    @Override
    public boolean track(String eventType, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean track(String trafficType, String eventType, double value, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean track(String trafficType, String eventType, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean track(String eventType, double value, Map<String, Object> properties) {
        return false;
    }

    @Override
    public boolean setAttribute(String attributeName, Object value) {
        return false;
    }

    @Nullable
    @Override
    public Object getAttribute(String attributeName) {
        return null;
    }

    @Override
    public boolean setAttributes(Map<String, Object> attributes) {
        return false;
    }

    @NonNull
    @Override
    public Map<String, Object> getAllAttributes() {
        return new HashMap<>();
    }

    @Override
    public void removeAttribute(String attributeName) {

    }

    @Override
    public void clearAttributes() {

    }
}
