package io.split.android.client.localhost;

import com.google.common.collect.Maps;

import io.split.android.client.utils.logger.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;

public class LocalhostSplitFile {

    private final File _file;

    public LocalhostSplitFile(String directory, String fileName) throws IOException {
        _file = new File(checkNotNull(directory), checkNotNull(fileName));
    }

    public Map<String, String> readOnSplits() throws IOException {
        Map<String, String> onSplits = Maps.newHashMap();

        try (BufferedReader reader = new BufferedReader(new FileReader(_file))) {
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] feature_treatment = line.split("\\s+");

                if (feature_treatment.length != 2) {
                    Logger.d("Ignoring line since it does not have exactly two columns: %s", line);
                    continue;
                }

                onSplits.put(feature_treatment[0], feature_treatment[1]);
                Logger.d("100%% of keys will see %s for %s", feature_treatment[1], feature_treatment[0]);
            }
        } catch (FileNotFoundException e) {
            Logger.w(e, "There was no file named %s found. " +
                    "We created a split client that returns default " +
                    "treatments for all features for all of your users. " +
                    "If you wish to return a specific treatment for a feature, " +
                    "enter the name of that feature name and treatment name separated " +
                    "by whitespace in %s; one pair per line. Empty lines or lines " +
                    "starting with '#' are considered comments", _file.getPath(), _file.getPath());
        }

        return onSplits;
    }
}