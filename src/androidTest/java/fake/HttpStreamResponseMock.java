package fake;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.BlockingQueue;

import io.split.android.client.network.BaseHttpResponseImpl;
import io.split.android.client.network.HttpStreamResponse;

@SuppressWarnings("ConstantConditions")
public class HttpStreamResponseMock extends BaseHttpResponseImpl implements HttpStreamResponse {

    final private BlockingQueue<String> mStreamingResponseData;
    final private PipedInputStream mInputStream;
    final private PipedOutputStream mOutputStream;
    final private BufferedReader mBufferedReader;
    private Thread mRedirectionThread;

    public HttpStreamResponseMock(int status, BlockingQueue<String> streamingResponseData) throws IOException {
        super(status);
        mStreamingResponseData = streamingResponseData;
        mInputStream = new PipedInputStream();
        mOutputStream = new PipedOutputStream(mInputStream);
        mBufferedReader = new BufferedReader(new InputStreamReader(mInputStream));
        startOutputRedirection();
    }

    @Override
    public BufferedReader getBufferedReader() {
        return mBufferedReader;
    }

    private void startOutputRedirection() throws IOException {

        mRedirectionThread = new Thread(new Runnable() {
            public void run () {
                try {
                    while(true) {
                        String data = mStreamingResponseData.take();
                        mOutputStream.write(data.getBytes());
                    }
                }
                catch (IOException e) {
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    if (mOutputStream != null) {
                        try {
                            mOutputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        mRedirectionThread.start();
    }

    public void close() {
        mRedirectionThread.interrupt();
        try {
            mBufferedReader.close();
            mOutputStream.close();
        } catch (IOException e) {
        }
    }
}
