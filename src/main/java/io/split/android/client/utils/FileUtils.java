package io.split.android.client.utils;

import android.content.Context;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import io.split.android.client.service.ServiceConstants;

public class FileUtils {

    public String loadFileContent(String name, Context context) throws IOException {
        String content;

        try {
            InputStream fin = context.getAssets().open(name);
            content = convertStreamToString(fin);
            fin.close();
        } catch (FileNotFoundException fnfe) {
            Logger.e("An error has ocurred: Could not find file " + name);
            throw fnfe;
        } catch (IOException ioe) {
            Logger.e("An error has ocurred: Could not open file " + name);
            throw ioe;
        }
        return content;
    }

    private String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;

        //noinspection ConstantConditions
        if (reader != null) {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
        }
        return sb.toString();
    }

    public boolean fileExists(String fileName, Context context) {
        String content = null;
        try {
            content = loadFileContent(fileName, context);
        } catch (IOException ignored) {
        }
        return content != null;
    }

    public boolean isPropertiesFileName(String fileName) {
        String propertiesExtension = ServiceConstants.PROPERTIES_EXTENSION;
        int propertiesLength = ServiceConstants.PROPERTIES_EXTENSION.length();
        if (propertiesLength < fileName.length()) {
            return fileName.substring(fileName.length() - propertiesLength).equals(propertiesExtension);
        }
        return false;
    }
}
