package io.split.android.client.network;

import androidx.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.nio.channels.SocketChannel;

import javax.net.ssl.SSLSocket;

import io.split.android.client.utils.logger.Logger;

/**
 * A transparent tunnel socket wrapper that maintains an SSL connection to a proxy
 * but presents a raw socket interface for origin server connections.
 * 
 * This solves the double-SSL layering problem by:
 * 1. Handling SSL proxy communication internally
 * 2. Presenting a standard Socket interface to external code
 * 3. Transparently forwarding data through the established tunnel
 * 
 * After CONNECT tunnel establishment, this socket appears as a direct connection
 * to the origin server, allowing proper SSL handshakes to be established on top.
 */
class TunnelSocket extends Socket {
    
    private final SSLSocket mProxySocket;
    private final String mTargetHost;
    private final int mTargetPort;
    private final InputStream mInputStream;
    private final OutputStream mOutputStream;
    private boolean mExplicitlyClosed = false;
    
    /**
     * Creates a tunnel socket that wraps an established SSL connection to a proxy.
     * 
     * @param proxySocket The SSL socket connected to the proxy (after CONNECT success)
     * @param targetHost The target host that the tunnel connects to
     * @param targetPort The target port that the tunnel connects to
     */
    public TunnelSocket(@NonNull SSLSocket proxySocket, @NonNull String targetHost, int targetPort) {
        Logger.v("TunnelSocket: Creating tunnel socket for " + targetHost + ":" + targetPort);
        this.mProxySocket = proxySocket;
        this.mTargetHost = targetHost;
        this.mTargetPort = targetPort;
        this.mInputStream = new TunnelInputStream();
        this.mOutputStream = new TunnelOutputStream();
        Logger.v("TunnelSocket: Created successfully, proxy socket connected: " + mProxySocket.isConnected());
    }
    
    // ========== Core Socket Methods ==========
    
    @Override
    public InputStream getInputStream() throws IOException {
        Logger.v("TunnelSocket: getInputStream() called, proxy socket connected: " + mProxySocket.isConnected());
        if (mProxySocket.isClosed()) {
            Logger.e("TunnelSocket: Proxy socket is closed when getInputStream() called!");
            throw new IOException("Proxy socket is closed");
        }
        return mInputStream;
    }
    
    @Override
    public OutputStream getOutputStream() throws IOException {
        Logger.v("TunnelSocket: getOutputStream() called, proxy socket connected: " + mProxySocket.isConnected());
        if (mProxySocket.isClosed()) {
            Logger.e("TunnelSocket: Proxy socket is closed when getOutputStream() called!");
            throw new IOException("Proxy socket is closed");
        }
        return mOutputStream;
    }
    
    @Override
    public void close() throws IOException {
        Logger.v("TunnelSocket: close() called - checking if should actually close proxy connection");
        
        // Don't immediately close the proxy connection when close() is called
        // SSLSocketFactory.createSocket() may call close() during socket creation
        // We only want to close when explicitly requested or when the tunnel is done
        
        // Mark as explicitly closed for future reference
        mExplicitlyClosed = true;
        
        // For now, don't close the proxy socket immediately
        // The proxy connection should remain open for the tunnel to work
        Logger.v("TunnelSocket: Marked as closed but keeping proxy connection alive for tunnel");
        
        // TODO: We may need a separate method for actually closing the proxy connection
        // when the tunnel is completely done (e.g., forceClose() or similar)
    }
    
    /**
     * Force close the underlying proxy connection.
     * This should only be called when the tunnel is completely done.
     */
    public void forceClose() throws IOException {
        Logger.v("TunnelSocket: forceClose() called - actually closing proxy connection");
        mProxySocket.close();
    }
    
    @Override
    public boolean isClosed() {
        // Return the state of the underlying proxy socket, not our explicit close flag
        boolean closed = mProxySocket.isClosed();
        Logger.v("TunnelSocket: isClosed() called, returning: " + closed);
        return closed;
    }
    
    @Override
    public boolean isConnected() {
        boolean connected = mProxySocket.isConnected() && !mProxySocket.isClosed();
        Logger.v("TunnelSocket: isConnected() called, returning: " + connected);
        return connected;
    }
    
    @Override
    public boolean isBound() {
        boolean bound = mProxySocket.isBound();
        Logger.v("TunnelSocket: isBound() called, returning: " + bound);
        return bound;
    }
    
    @Override
    public boolean isInputShutdown() {
        Logger.v("TunnelSocket: isInputShutdown() called");
        try {
            return mProxySocket.isInputShutdown();
        } catch (Exception e) {
            Logger.e("TunnelSocket: Error checking input shutdown: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public boolean isOutputShutdown() {
        Logger.v("TunnelSocket: isOutputShutdown() called");
        try {
            return mProxySocket.isOutputShutdown();
        } catch (Exception e) {
            Logger.e("TunnelSocket: Error checking output shutdown: " + e.getMessage());
            return false;
        }
    }
    
    @Override
    public void shutdownInput() throws IOException {
        Logger.v("TunnelSocket: shutdownInput() called - NOT shutting down proxy input");
        // Don't shutdown the proxy socket input as it would break the tunnel
        // throw new UnsupportedOperationException("Cannot shutdown input on tunnel socket");
    }
    
    @Override
    public void shutdownOutput() throws IOException {
        Logger.v("TunnelSocket: shutdownOutput() called - NOT shutting down proxy output");
        // Don't shutdown the proxy socket output as it would break the tunnel
        // throw new UnsupportedOperationException("Cannot shutdown output on tunnel socket");
    }
    
    // ========== Address Information ==========
    
    @Override
    public InetAddress getInetAddress() {
        // Return the target address, not the proxy address
        try {
            return InetAddress.getByName(mTargetHost);
        } catch (Exception e) {
            return null;
        }
    }
    
    @Override
    public int getPort() {
        return mTargetPort;
    }
    
    @Override
    public InetAddress getLocalAddress() {
        return mProxySocket.getLocalAddress();
    }
    
    @Override
    public int getLocalPort() {
        return mProxySocket.getLocalPort();
    }
    
    @Override
    public SocketAddress getRemoteSocketAddress() {
        // This should represent the target, but we'll delegate to proxy for now
        return mProxySocket.getRemoteSocketAddress();
    }
    
    @Override
    public SocketAddress getLocalSocketAddress() {
        return mProxySocket.getLocalSocketAddress();
    }
    
    // ========== Socket Options ==========
    
    @Override
    public void setSoTimeout(int timeout) throws SocketException {
        try {
            mProxySocket.setSoTimeout(timeout);
        } catch (SocketException e) {
            throw e;
        } catch (Exception e) {
            throw new SocketException(e.getMessage());
        }
    }
    
    @Override
    public int getSoTimeout() throws SocketException {
        try {
            return mProxySocket.getSoTimeout();
        } catch (SocketException e) {
            throw e;
        } catch (Exception e) {
            throw new SocketException(e.getMessage());
        }
    }
    
    @Override
    public void setTcpNoDelay(boolean on) throws SocketException {
        try {
            mProxySocket.setTcpNoDelay(on);
        } catch (SocketException e) {
            throw e;
        } catch (Exception e) {
            throw new SocketException(e.getMessage());
        }
    }
    
    @Override
    public boolean getTcpNoDelay() throws SocketException {
        try {
            return mProxySocket.getTcpNoDelay();
        } catch (SocketException e) {
            throw e;
        } catch (Exception e) {
            throw new SocketException(e.getMessage());
        }
    }
    
    // ========== Unsupported Operations ==========
    
    @Override
    public void connect(SocketAddress endpoint) throws IOException {
        Logger.v("TunnelSocket: connect() called, but tunnel socket is already connected through proxy");
        throw new UnsupportedOperationException("TunnelSocket is already connected through proxy");
    }
    
    @Override
    public void connect(SocketAddress endpoint, int timeout) throws IOException {
        Logger.v("TunnelSocket: connect() called, but tunnel socket is already connected through proxy");
        throw new UnsupportedOperationException("TunnelSocket is already connected through proxy");
    }
    
    @Override
    public void bind(SocketAddress bindpoint) throws IOException {
        Logger.v("TunnelSocket: bind() called, but tunnel socket cannot be rebound");
        throw new UnsupportedOperationException("TunnelSocket cannot be rebound");
    }
    
    @Override
    public SocketChannel getChannel() {
        Logger.v("TunnelSocket: getChannel() called, but not supported for tunnel sockets");
        return null; // Not supported for tunnel sockets
    }
    
    // ========== Stream Implementations ==========
    
    /**
     * InputStream that reads from the SSL proxy socket, transparently
     * forwarding data from the tunneled connection.
     */
    private class TunnelInputStream extends InputStream {
        
        @Override
        public int read() throws IOException {
            Logger.v("TunnelSocket: TunnelInputStream read() called");
            if (mProxySocket.isClosed()) {
                Logger.e("TunnelSocket: Proxy socket is closed during read()!");
                throw new IOException("Proxy socket is closed");
            }
            try {
                int result = mProxySocket.getInputStream().read();
                Logger.v("TunnelSocket: TunnelInputStream read() returning: " + result);
                return result;
            } catch (IOException e) {
                Logger.e("TunnelSocket: TunnelInputStream read() failed: " + e.getMessage());
                throw e;
            }
        }
        
        @Override
        public int read(byte[] b) throws IOException {
            Logger.v("TunnelSocket: TunnelInputStream read(byte[]) called, length: " + b.length);
            if (mProxySocket.isClosed()) {
                Logger.e("TunnelSocket: Proxy socket is closed during read(byte[])!");
                throw new IOException("Proxy socket is closed");
            }
            try {
                int result = mProxySocket.getInputStream().read(b);
                Logger.v("TunnelSocket: TunnelInputStream read(byte[]) returning: " + result);
                return result;
            } catch (IOException e) {
                Logger.e("TunnelSocket: TunnelInputStream read(byte[]) failed: " + e.getMessage());
                throw e;
            }
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            Logger.v("TunnelSocket: TunnelInputStream read(byte[], int, int) called, off: " + off + ", len: " + len);
            if (mProxySocket.isClosed()) {
                Logger.e("TunnelSocket: Proxy socket is closed during read(byte[], int, int)!");
                throw new IOException("Proxy socket is closed");
            }
            try {
                int result = mProxySocket.getInputStream().read(b, off, len);
                Logger.v("TunnelSocket: TunnelInputStream read(byte[], int, int) returning: " + result);
                return result;
            } catch (IOException e) {
                Logger.e("TunnelSocket: TunnelInputStream read(byte[], int, int) failed: " + e.getMessage());
                throw e;
            }
        }
        
        @Override
        public long skip(long n) throws IOException {
            Logger.v("TunnelSocket: TunnelInputStream skip() called");
            if (mProxySocket.isClosed()) {
                Logger.e("TunnelSocket: Proxy socket is closed during skip()!");
                throw new IOException("Proxy socket is closed");
            }
            return mProxySocket.getInputStream().skip(n);
        }
        
        @Override
        public int available() throws IOException {
            Logger.v("TunnelSocket: TunnelInputStream available() called");
            if (mProxySocket.isClosed()) {
                Logger.e("TunnelSocket: Proxy socket is closed during available()!");
                throw new IOException("Proxy socket is closed");
            }
            int result = mProxySocket.getInputStream().available();
            Logger.v("TunnelSocket: TunnelInputStream available() returning: " + result);
            return result;
        }
        
        @Override
        public void close() throws IOException {
            Logger.v("TunnelSocket: TunnelInputStream close() called");
            // Don't close the underlying stream directly, let TunnelSocket handle it
        }
        
        @Override
        public void mark(int readlimit) {
            Logger.v("TunnelSocket: TunnelInputStream mark() called");
            try {
                mProxySocket.getInputStream().mark(readlimit);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        
        @Override
        public void reset() throws IOException {
            Logger.v("TunnelSocket: TunnelInputStream reset() called");
            mProxySocket.getInputStream().reset();
        }
        
        @Override
        public boolean markSupported() {
            Logger.v("TunnelSocket: TunnelInputStream markSupported() called");
            try {
                return mProxySocket.getInputStream().markSupported();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    /**
     * OutputStream that writes to the SSL proxy socket, transparently
     * forwarding data through the tunneled connection.
     */
    private class TunnelOutputStream extends OutputStream {
        
        @Override
        public void write(int b) throws IOException {
            Logger.v("TunnelSocket: TunnelOutputStream write(int) called");
            if (mProxySocket.isClosed()) {
                Logger.e("TunnelSocket: Proxy socket is closed during write(int)!");
                throw new IOException("Proxy socket is closed");
            }
            try {
                mProxySocket.getOutputStream().write(b);
                Logger.v("TunnelSocket: TunnelOutputStream write(int) completed");
            } catch (IOException e) {
                Logger.e("TunnelSocket: TunnelOutputStream write(int) failed: " + e.getMessage());
                throw e;
            }
        }
        
        @Override
        public void write(byte[] b) throws IOException {
            Logger.v("TunnelSocket: TunnelOutputStream write(byte[]) called, length: " + b.length);
            if (mProxySocket.isClosed()) {
                Logger.e("TunnelSocket: Proxy socket is closed during write(byte[])!");
                throw new IOException("Proxy socket is closed");
            }
            try {
                mProxySocket.getOutputStream().write(b);
                Logger.v("TunnelSocket: TunnelOutputStream write(byte[]) completed");
            } catch (IOException e) {
                Logger.e("TunnelSocket: TunnelOutputStream write(byte[]) failed: " + e.getMessage());
                throw e;
            }
        }
        
        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            Logger.v("TunnelSocket: TunnelOutputStream write(byte[], int, int) called, off: " + off + ", len: " + len);
            if (mProxySocket.isClosed()) {
                Logger.e("TunnelSocket: Proxy socket is closed during write(byte[], int, int)!");
                throw new IOException("Proxy socket is closed");
            }
            try {
                mProxySocket.getOutputStream().write(b, off, len);
                Logger.v("TunnelSocket: TunnelOutputStream write(byte[], int, int) completed");
            } catch (IOException e) {
                Logger.e("TunnelSocket: TunnelOutputStream write(byte[], int, int) failed: " + e.getMessage());
                throw e;
            }
        }
        
        @Override
        public void flush() throws IOException {
            Logger.v("TunnelSocket: TunnelOutputStream flush() called");
            if (mProxySocket.isClosed()) {
                Logger.e("TunnelSocket: Proxy socket is closed during flush()!");
                throw new IOException("Proxy socket is closed");
            }
            try {
                mProxySocket.getOutputStream().flush();
                Logger.v("TunnelSocket: TunnelOutputStream flush() completed");
            } catch (IOException e) {
                Logger.e("TunnelSocket: TunnelOutputStream flush() failed: " + e.getMessage());
                throw e;
            }
        }
        
        @Override
        public void close() throws IOException {
            Logger.v("TunnelSocket: TunnelOutputStream close() called");
            // Don't close the underlying stream directly, let TunnelSocket handle it
        }
    }
}
