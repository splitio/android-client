package io.split.android.telemetry.model;

public class UrlOverrides {

    private boolean sdkUrl;

    private boolean events;

    private boolean auth;

    private boolean stream;

    private boolean telemetry;

    public boolean isSdkUrl() {
        return sdkUrl;
    }

    public void setSdkUrl(boolean sdkUrl) {
        this.sdkUrl = sdkUrl;
    }

    public boolean isEvents() {
        return events;
    }

    public void setEvents(boolean events) {
        this.events = events;
    }

    public boolean isAuth() {
        return auth;
    }

    public void setAuth(boolean auth) {
        this.auth = auth;
    }

    public boolean isStream() {
        return stream;
    }

    public void setStream(boolean stream) {
        this.stream = stream;
    }

    public boolean isTelemetry() {
        return telemetry;
    }

    public void setTelemetry(boolean telemetry) {
        this.telemetry = telemetry;
    }
}
