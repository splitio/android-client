package io.split.android.client.dtos;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import io.split.android.client.network.BasicCredentialsProvider;
import io.split.android.client.network.BearerCredentialsProvider;

/**
 * DTO for HttpProxy serialization to JSON for storage in GeneralInfoStorage.
 */
public class HttpProxyDto {

    @SerializedName("host")
    public String host;

    @SerializedName("port")
    public int port;

    @SerializedName("username")
    public String username;

    @SerializedName("password")
    public String password;

    @SerializedName("client_cert")
    public String clientCert;

    @SerializedName("client_key")
    public String clientKey;

    @SerializedName("ca_cert")
    public String caCert;

    @SerializedName("bearer_token")
    public String bearerToken;

    public HttpProxyDto() {
        // Default constructor for deserialization
    }

    /**
     * Constructor that creates a DTO from an HttpProxy instance.
     * Note that we don't store the actual stream data, only whether they exist.
     *
     * @param httpProxy The HttpProxy instance to convert
     */
    public HttpProxyDto(@NonNull io.split.android.client.network.HttpProxy httpProxy) {
        this.host = httpProxy.getHost();
        this.port = httpProxy.getPort();
        if (httpProxy.getCredentialsProvider() instanceof BasicCredentialsProvider) {
            BasicCredentialsProvider provider = (BasicCredentialsProvider) httpProxy.getCredentialsProvider();
            this.username = provider.getUsername();
            this.password = provider.getPassword();
        } else if (httpProxy.getCredentialsProvider() instanceof BearerCredentialsProvider) {
            BearerCredentialsProvider provider = (BearerCredentialsProvider) httpProxy.getCredentialsProvider();
            this.bearerToken = provider.getToken();
        }

        this.clientCert = streamToString(httpProxy.getClientCertStream());
        this.clientKey = streamToString(httpProxy.getClientKeyStream());
        this.caCert = streamToString(httpProxy.getCaCertStream());
    }
    
    /**
     * Converts an InputStream to a String.
     * 
     * @param inputStream The InputStream to convert
     * @return String representation of the InputStream contents, or null if the stream is null
     */
    @Nullable
    private String streamToString(@Nullable InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        
        try {
            StringBuilder content = getStringBuilder(inputStream);

            // Reset the stream if possible to allow reuse
            try {
                inputStream.reset();
            } catch (IOException ignored) {

            }
            return content.toString();
        } catch (Exception e) {
            return null;
        }
    }

    @NonNull
    private static StringBuilder getStringBuilder(@NonNull InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder content = new StringBuilder();
        String line;
        boolean firstLine = true;

        while ((line = reader.readLine()) != null) {
            if (!firstLine) {
                content.append("\n");
            } else {
                firstLine = false;
            }
            content.append(line);
        }
        return content;
    }
}
