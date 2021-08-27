package tests.service;

import android.content.Context;

import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import helper.CompressionHelper;
import helper.FileHelper;
import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.CompressionUtil;
import io.split.android.client.utils.Gzip;
import io.split.android.client.utils.StringHelper;
import io.split.android.client.utils.Zlib;

public class CompressionTest {

    CompressionUtil zlib;
    CompressionUtil gzip;
    CompressionHelper helper;
    Context mContext;

    @Before
    public void setup() {
        mContext = InstrumentationRegistry.getInstrumentation().getContext();
        zlib = new Zlib();
        gzip = new Gzip();
        helper = new CompressionHelper();
    }

    @Test
    public void zlibBasicDecompress() {
        String toComp = "0123456789_0123456789";
        byte[] compressed = helper.compressZlib(toComp.getBytes(StringHelper.defaultCharset()));
        byte[] dec = zlib.decompress(compressed);
        Assert.assertEquals(toComp, new String(dec, 0, dec.length));
    }

    @Test
    public void zlibManyDecompress() {
        for(int i = 0; i < 20; i++) {
            String toComp = generateString();
            byte[] compressed = helper.compressZlib(toComp.getBytes(StringHelper.defaultCharset()));
            byte[] dec = zlib.decompress(compressed);
            Assert.assertEquals(toComp, new String(dec, 0, dec.length));
        }
    }

    @Test
    public void zlibLoremIpsum() {
        List<String> params = loadFileContent();
        for(String p : params) {
            byte[] compressed = helper.compressZlib(p.getBytes(StringHelper.defaultCharset()));
            byte[] dec = zlib.decompress(compressed);
            Assert.assertEquals(p, new String(dec, 0, dec.length));
        }
    }

    @Test
    public void zlibBase64Decompression() {
        for(int i = 0; i < 20; i++) {
            String toComp = Base64Util.encode(generateString());
            byte[] compressed = helper.compressZlib(toComp.getBytes(StringHelper.defaultCharset()));
            byte[] dec = zlib.decompress(compressed);
            Assert.assertEquals(toComp, new String(dec, 0, dec.length));
        }
    }

    @Test
    public void gzipBasicDecompress() {
        String toComp = "0123456789_0123456789";
        byte[] compressed = helper.compressGzip(toComp.getBytes(StringHelper.defaultCharset()));
        byte[] dec = gzip.decompress(compressed);
        Assert.assertEquals(toComp, new String(dec, 0, dec.length));
    }

    @Test
    public void gzipManyDecompress() {
        for(int i = 0; i < 20; i++) {
            String toComp = generateString();
            byte[] compressed = helper.compressGzip(toComp.getBytes(StringHelper.defaultCharset()));
            byte[] dec = gzip.decompress(compressed);
            Assert.assertEquals(toComp, new String(dec, 0, dec.length));
        }
    }

    @Test
    public void gzipLoremIpsum() {
        List<String> params = loadFileContent();
        for(String p : params) {
            byte[] compressed = helper.compressGzip(p.getBytes(StringHelper.defaultCharset()));
            byte[] dec = gzip.decompress(compressed);
            Assert.assertEquals(p, new String(dec, 0, dec.length));
        }
    }

    @Test
    public void gzipBase64Decompression() {
        for(int i = 0; i < 20; i++) {
            String toComp = Base64Util.encode(generateString());
            byte[] compressed = helper.compressGzip(toComp.getBytes(StringHelper.defaultCharset()));
            byte[] dec = gzip.decompress(compressed);
            Assert.assertEquals(toComp, new String(dec, 0, dec.length));
        }
    }

    private String generateString() {
        StringBuilder str = new StringBuilder();
        for(int i = 0; i < 20; i++) {
            str.append(UUID.randomUUID());
        }
        return str.toString();
    }

    private List<String> loadFileContent() {
        FileHelper fileHelper = new FileHelper();
        String content = fileHelper.loadFileContent(mContext, "lorem_ipsum.txt");
        return Arrays.asList(content.split("\n"));
    }
}


