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
import io.split.android.client.dtos.TargetingRulesChange;
import io.split.android.client.utils.Json;

public class FileHelper {

    public List<Split> loadAndParseSplitChangeFile(String name) {
        try {
            String content = loadFileContent(name);
            TargetingRulesChange change = Json.fromJson(content, TargetingRulesChange.class);
            return change.getFeatureFlagsChange().splits;
        } catch (Exception e) {
            System.out.println("loadAndParseSplitChangeFile: Failed load file content" + e.getLocalizedMessage());
        }
        return null;
    }

    public SplitChange loadSplitChangeFromFile(String name) {
        try {
            String content = loadFileContent(name);
            TargetingRulesChange change = Json.fromJson(content, TargetingRulesChange.class);
            return change.getFeatureFlagsChange();
        } catch (Exception e) {
        }
        return null;
    }

    public String loadFileContent(String name) {
        ClassLoader classLoader = this.getClass().getClassLoader();
        URL resource = classLoader.getResource(name);
        File file = new File(resource.getPath());
        String content = null;
        try {
            FileInputStream fin = new FileInputStream(file);
            content = convertStreamToString(fin);
            fin.close();
        } catch (Exception e) {
        }
        return content;
    }

    private String convertStreamToString(InputStream is) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        return sb.toString();
    }
}
