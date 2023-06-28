package io.split.android.client.service.sseclient.notifications;

import com.google.gson.annotations.SerializedName;

public class RawNotification {

    @SerializedName("clientId")
    private String clientId;
    @SerializedName("name")
    private String name;
    @SerializedName("data")
    private String data;
    @SerializedName("channel")
    private String channel;
    @SerializedName("timestamp")
    private long timestamp;

    public String getClientId() {
        return clientId;
    }

    public String getData() {
        return data;
    }

    public String getChannel() {
        return channel;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public String getName() {
        return name;
    }
}
