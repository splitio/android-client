package io.split.android.helpers;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.List;

import io.split.android.client.dtos.Split;
import io.split.android.client.dtos.SplitChange;
import io.split.android.client.utils.Json;

public class FileHelper {

    public List<Split> loadAndParseSplitChangeFile (String name) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL resource = classLoader.getResource(name);
        File file = new File(resource.getPath());
        String content = null;
        try {
            FileInputStream fin = new FileInputStream(file);
            content = convertStreamToString(fin);
            fin.close();
            SplitChange change = Json.fromJson(content, SplitChange.class);
            List<Split> splits = change.splits;
            return splits;
        } catch (Exception e) {
        }
        return null;
    }

    public String 

    private String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;

        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }
}
