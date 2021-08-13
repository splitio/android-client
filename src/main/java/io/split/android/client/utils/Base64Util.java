package io.split.android.client.utils;

import android.util.Base64;

import androidx.annotation.Nullable;

import java.nio.charset.Charset;

public class Base64Util {
    @Nullable
    public static String decode(String string) {
        byte[] bytes = bytesDecode(string);
        if (bytes != null) {
            return StringHelper.stringFromBytes(bytes);
        }
        return null;
    }

    @Nullable
    public static byte[] bytesDecode(String string) {
        String decoded = null;
        try {
            return Base64.decode(string, Base64.DEFAULT);
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
            byte[] bytes = Base64.encode(string.getBytes(StringHelper.defaultCharset()), Base64.DEFAULT);
            return StringHelper.stringFromBytes(bytes);
        } catch (IllegalArgumentException e) {
            Logger.e("Received bytes didn't correspond to a valid Base64 encoded string." + e.getLocalizedMessage());
        } catch (Exception e) {
            Logger.e("An unknown error has ocurred " + e.getLocalizedMessage());
        }
        return null;
    }
}
