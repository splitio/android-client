package io.split.android.client.network;

import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.Socket;

import io.split.android.client.utils.logger.Logger;

public class HttpStreamResponseImpl extends BaseHttpResponseImpl implements HttpStreamResponse {

    private final BufferedReader mData;

    // Sockets are referenced when using Proxy tunneling, in order to close them
    @Nullable
    private final Socket mTunnelSocket;
    @Nullable
    private final Socket mOriginSocket;

    private HttpStreamResponseImpl(int httpStatus, BufferedReader data,
                                  @Nullable Socket tunnelSocket,
                                  @Nullable Socket originSocket) {
        super(httpStatus);
        mData = data;
        mTunnelSocket = tunnelSocket;
        mOriginSocket = originSocket;
    }

    static HttpStreamResponseImpl createFromTunnelSocket(int httpStatus,
                                                  BufferedReader data,
                                                  @Nullable Socket tunnelSocket,
                                                  @Nullable Socket originSocket) {
        return new HttpStreamResponseImpl(httpStatus, data, tunnelSocket, originSocket);
    }

    static HttpStreamResponseImpl createFromHttpUrlConnection(int httpStatus, BufferedReader data) {
        return new HttpStreamResponseImpl(httpStatus, data, null, null);
    }

    @Override
    @Nullable
    public BufferedReader getBufferedReader() {
        return mData;
    }

    @Override
    public void close() throws IOException {

        // Close the BufferedReader first
        if (mData != null) {
            try {
                mData.close();
            } catch (IOException e) {
                Logger.w("Failed to close BufferedReader: " + e.getMessage());
            }
        }

        // Close origin socket if it exists and is different from tunnel socket
        if (mOriginSocket != null && mOriginSocket != mTunnelSocket) {
            try {
                mOriginSocket.close();
                Logger.v("Origin socket closed");
            } catch (IOException e) {
                Logger.w("Failed to close origin socket: " + e.getMessage());
            }
        }

        // Close tunnel socket
        if (mTunnelSocket != null) {
            try {
                mTunnelSocket.close();
                Logger.v("Tunnel socket closed");
            } catch (IOException e) {
                Logger.w("Failed to close tunnel socket: " + e.getMessage());
            }
        }
    }
}
