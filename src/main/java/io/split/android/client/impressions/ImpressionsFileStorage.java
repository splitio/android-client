package io.split.android.client.impressions;

import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;

import io.split.android.client.dtos.ChunkHeader;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.storage.FileStorage;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class ImpressionsFileStorage extends FileStorage implements IImpressionsStorage {

    private static final String FILE_NAME_PREFIX = "SPLITIO.impressions_chunk_id_";
    private static final String FILE_NAME_TEMPLATE = FILE_NAME_PREFIX + "%s.jsonl";
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    private final static Type IMPRESSION_CHUNK_TYPE = new TypeToken<ChunkHeader>() {
    }.getType();
    private final static Type IMPRESSION_ROW_TYPE = new TypeToken<KeyImpression>() {
    }.getType();

    public ImpressionsFileStorage(@NotNull File rootFolder, @NotNull String folderName) {
        super(rootFolder, folderName);
    }

    public Map<String, StoredImpressions> read() throws IOException {

        Map<String, StoredImpressions> impressions = new HashMap<>();
        List<String> impressionFiles = getAllIds(FILE_NAME_PREFIX);

        for (String fileName : impressionFiles) {
            FileInputStream inputStream = null;
            Scanner sc = null;
            try {
                File chunkFile = new File(_dataFolder, fileName);
                inputStream = new FileInputStream(chunkFile);
                sc = new Scanner(inputStream, "UTF-8");
                StoredImpressions impressionsChunk = null;
                if (sc.hasNextLine()) {
                    ChunkHeader chunkHeader = null;
                    String chunkLine = sc.nextLine();
                    if (!Strings.isNullOrEmpty(chunkLine)) {
                        try {
                            chunkHeader = Json.fromJson(chunkLine, IMPRESSION_CHUNK_TYPE);
                        } catch (JsonSyntaxException e) {
                            chunkHeader = new ChunkHeader(UUID.randomUUID().toString(), 1);
                        }
                    } else {
                        continue;
                    }
                    if (chunkHeader != null) {
                        impressionsChunk = StoredImpressions.from(chunkHeader.getId(), chunkHeader.getAttempt(), chunkHeader.getTimestamp());
                        List<TestImpressions> testImpressions = new ArrayList<>();
                        TestImpressions testImpressionsRow = new TestImpressions();
                        String testName = null;
                        while (sc.hasNextLine()) {
                            String jsonImpression = null;
                            try {
                                jsonImpression = sc.nextLine();
                                KeyImpression keyImpression = Json.fromJson(jsonImpression, IMPRESSION_ROW_TYPE);
                                if(keyImpression != null) {
                                    if (keyImpression.feature != null && !keyImpression.feature.equals(testName)) {
                                        if (testName != null) {
                                            testImpressions.add(testImpressionsRow);
                                            testImpressionsRow = new TestImpressions();
                                        }
                                        testName = keyImpression.feature;
                                        testImpressionsRow.testName = testName;
                                        testImpressionsRow.keyImpressions = new ArrayList<>();
                                    }
                                    testImpressionsRow.keyImpressions.add(keyImpression);
                                }
                            } catch (JsonSyntaxException e){
                                Logger.e("Could not parse event: " + jsonImpression + " from file: " + fileName);
                            }
                        }
                        testImpressions.add(testImpressionsRow);
                        impressionsChunk.addImpressions(testImpressions);
                    }
                }
                if(impressionsChunk.impressions().size() > 0) {
                    impressions.put(impressionsChunk.id(), impressionsChunk);
                }

                if (sc.ioException() != null) {
                    Logger.e("An error occurs parsing track events from JsonL files: " + sc.ioException().getLocalizedMessage());
                }

            } catch (FileNotFoundException e) {
                Logger.w("No cached track files found");
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (sc != null) {
                    sc.close();
                }
            }
        }
        for (String fileName : impressionFiles) {
            delete(fileName);
        }
        return impressions;
    }

    public void write(Map<String, StoredImpressions> impressions) throws IOException {

        for (StoredImpressions chunk : impressions.values()) {
            FileWriter fileWriter = null;
            try {
                String fileName = String.format(FILE_NAME_TEMPLATE, chunk.id());
                File file = new File(_dataFolder, fileName);
                fileWriter = new FileWriter(file);
                ChunkHeader chunkHeader = new ChunkHeader(chunk.id(), chunk.getAttempts(), chunk.getTimestamp());
                String jsonChunkHeader = Json.toJson(chunkHeader);
                fileWriter.write(String.format(jsonChunkHeader));
                fileWriter.write(LINE_SEPARATOR);
                List<TestImpressions> testImpressions = chunk.impressions();
                if (testImpressions != null) {
                    for (TestImpressions testImpressionsRow : testImpressions) {
                        List<KeyImpression> keyImpressions = testImpressionsRow.keyImpressions;
                        if (keyImpressions != null) {
                            for (KeyImpression keyImpression : keyImpressions) {
                                String jsonImpression = Json.toJson(keyImpression);
                                fileWriter.write(jsonImpression);
                                fileWriter.write(LINE_SEPARATOR);
                            }
                        }
                    }
                }
            } catch (IOException ex) {
                throw new IOException("Error writing track events chunk: " + FILE_NAME_TEMPLATE);
            } finally {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            }
        }
    }
}
