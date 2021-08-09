package io.split.android.client.utils;

import android.util.Base64;

import androidx.annotation.Nullable;

import java.nio.charset.Charset;

public class Base64Util {
    @Nullable
    public static String decode(String string) {
        String decoded = null;
        try {
            byte[] bytes = Base64.decode(string, Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            return new String(bytes, Charset.defaultCharset());
        } catch (IllegalArgumentException e) {
            Logger.e("Received bytes didn't correspond to a valid Base64 encoded string." + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("An unknown error has ocurred " + e.getLocalizedMessage());
        }
        return null;
    }

    @Nullable
    public static String encode(String string) {
        try {
            //byte[] bytes = Base64.encode(string.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP | Base64.NO_PADDING);
            byte[] bytes = Base64.encode(string.getBytes(Charset.defaultCharset()), Base64.DEFAULT);
            return new String(bytes, Charset.defaultCharset());
        } catch (IllegalArgumentException e) {
            Logger.e("Received bytes didn't correspond to a valid Base64 encoded string." + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("An unknown error has ocurred " + e.getLocalizedMessage());
        }
        return null;
    }
}
