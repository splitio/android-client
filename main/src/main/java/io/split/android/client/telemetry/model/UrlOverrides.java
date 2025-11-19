package io.split.android.client.telemetry.model;

import com.google.gson.annotations.SerializedName;

public class UrlOverrides {

    @SerializedName("s")
    private boolean sdkUrl;

    @SerializedName("e")
    private boolean events;

    @SerializedName("a")
    private boolean auth;

    @SerializedName("st")
    private boolean stream;

    @SerializedName("t")
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
