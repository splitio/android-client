package io.split.android.client.impressions;

import com.google.common.base.Strings;
import com.google.gson.JsonSyntaxException;

import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import io.split.android.client.dtos.ChunkHeader;
import io.split.android.client.dtos.KeyImpression;
import io.split.android.client.dtos.TestImpressions;
import io.split.android.client.storage.FileStorage;
import io.split.android.client.storage.FileStorageHelper;
import io.split.android.client.utils.Json;
import io.split.android.client.utils.Logger;

public class ImpressionsFileStorage extends FileStorage implements IImpressionsStorage {

    private static final String FILE_NAME_PREFIX = "SPLITIO.impressions_chunk_id_";
    private static final String FILE_NAME_TEMPLATE = FILE_NAME_PREFIX + "%s.jsonl";
    private FileStorageHelper _fileStorageHelper;

    public ImpressionsFileStorage(@NotNull File rootFolder, @NotNull String folderName) {
        super(rootFolder, folderName);
        _fileStorageHelper = new FileStorageHelper();
    }

    public Map<String, StoredImpressions> read() {

        Map<String, StoredImpressions> impressions = new HashMap<>();
        List<String> impressionFiles = getAllIds(FILE_NAME_PREFIX);

        for (String fileName : impressionFiles) {
            FileInputStream inputStream = null;
            Scanner scanner = null;
            try {
                inputStream = new FileInputStream(new File(_dataFolder, fileName));
                scanner = new Scanner(inputStream, FileStorageHelper.UTF8_CHARSET);
                StoredImpressions impressionsChunk = null;
                if (scanner.hasNextLine()) {
                    ChunkHeader chunkHeader = _fileStorageHelper.chunkFromLine(scanner.nextLine());
                    impressionsChunk = StoredImpressions.from(chunkHeader.getId(), chunkHeader.getAttempt(), chunkHeader.getTimestamp());
                    List<TestImpressions> testImpressions = new ArrayList<>();
                    TestImpressions testImpressionsRow = new TestImpressions();
                    String testName = null;
                    while (scanner.hasNextLine()) {
                        KeyImpression keyImpression = keyImpressionFromLine(scanner.nextLine());
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
                    }
                    testImpressions.add(testImpressionsRow);
                    impressionsChunk.addImpressions(testImpressions);
                }

                if(impressionsChunk.impressions().size() > 0) {
                    impressions.put(impressionsChunk.id(), impressionsChunk);
                }
                _fileStorageHelper.logIfScannerException(scanner, "An error occurs parsing track events from JsonL files");

            } catch (FileNotFoundException e) {
                Logger.w("No cached impressions files found");
            } finally {
                _fileStorageHelper.closeFileInputStream(inputStream);
                _fileStorageHelper.closeScanner(scanner);
            }
        }
        delete(impressionFiles);
        return impressions;
    }

    public void write(Map<String, StoredImpressions> impressions) {

        for (StoredImpressions chunk : impressions.values()) {
            FileWriter fileWriter = null;
            try {
                fileWriter = _fileStorageHelper.fileWriterFrom(_dataFolder, String.format(FILE_NAME_TEMPLATE, chunk.id()));
                ChunkHeader chunkHeader = new ChunkHeader(chunk.id(), chunk.getAttempts(), chunk.getTimestamp());
                _fileStorageHelper.writeChunkHeaderLine(chunkHeader, fileWriter);
                List<TestImpressions> testImpressions = chunk.impressions();
                if (testImpressions != null) {
                    for (TestImpressions testImpressionsRow : testImpressions) {
                        List<KeyImpression> keyImpressions = testImpressionsRow.keyImpressions;
                        if (keyImpressions != null) {
                            for (KeyImpression keyImpression : keyImpressions) {
                                writeImpressionLine(keyImpression, fileWriter);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Logger.e("Error writing impressions chunk: " + FILE_NAME_TEMPLATE + ": " + e.getLocalizedMessage());
            } finally {
                _fileStorageHelper.closeFileWriter(fileWriter);
            }
        }
    }

    private void writeImpressionLine(KeyImpression impression, FileWriter fileWriter) throws IOException {
        String jsonImpression = Json.toJson(impression);
        fileWriter.write(jsonImpression);
        fileWriter.write(FileStorageHelper.LINE_SEPARATOR);
    }

    private KeyImpression keyImpressionFromLine(String jsonImpression) {

        if(Strings.isNullOrEmpty(jsonImpression)) {
            return null;
        }

        KeyImpression impression = null;
        try {
            impression = Json.fromJson(jsonImpression, KeyImpression.class);
        } catch (JsonSyntaxException e){
            Logger.e("Could not parse impression: " + jsonImpression);
        }
        return impression;
    }
}
