package io.split.android.client.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class FileUtils {

    public String loadFileContent(String name) throws IOException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL resource = classLoader.getResource(name);
        File file = new File(resource.getPath());
        String content = null;

        FileInputStream fin = null;
        try {
            fin = new FileInputStream(file);
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

    String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;

        if (reader != null) {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            reader.close();
        }
        return sb.toString();
    }

    public InputStream fileStream(String name) throws IOException {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL resource = classLoader.getResource(name);
        File file = new File(resource.getPath());
        String content = null;

        FileInputStream fin = null;
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException fnfe) {
            Logger.e("An error has ocurred: Could not find file " + name);
            throw fnfe;
        } catch (IOException ioe) {
            Logger.e("An error has ocurred: Could not open file " + name);
            throw ioe;
        }
    }
}
