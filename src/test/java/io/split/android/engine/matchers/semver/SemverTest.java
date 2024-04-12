package io.split.android.engine.matchers.semver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;

public class SemverTest {

    private static final String VALID_SEMANTIC_VERSIONS_CSV = "valid_semantic_versions.csv";
    private static final String INVALID_SEMANTIC_VERSIONS_CSV = "invalid_semantic_versions.csv";
    private static final String EQUAL_TO_SEMVER_CSV = "equal_to_semver.csv";
    private static final String BETWEEN_SEMVER_CSV = "between_semver.csv";

    @Test
    public void greaterThanOrEqualTo() throws IOException {
        try (BufferedReader reader = getReaderForFile(VALID_SEMANTIC_VERSIONS_CSV, getClass().getClassLoader())) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                Semver version1 = Semver.build(parts[0]);
                Semver version2 = Semver.build(parts[1]);

                assertTrue(version1.compare(version2) >= 0);
                assertFalse(version2.compare(version1) >= 0);
                assertEquals(0, version1.compare(version1));
                assertEquals(0, version2.compare(version2));
            }
        }
    }

    @Test
    public void lessThanOrEqualTo() throws IOException {
        try (BufferedReader reader = getReaderForFile(VALID_SEMANTIC_VERSIONS_CSV, getClass().getClassLoader())) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                Semver version1 = Semver.build(parts[0]);
                Semver version2 = Semver.build(parts[1]);

                assertFalse(version1.compare(version2) <= 0);
                assertTrue(version2.compare(version1) <= 0);
                assertEquals(0, version1.compare(version1));
                assertEquals(0, version2.compare(version2));
            }
        }
    }

    @Test
    public void invalidFormats() throws IOException {
        try (BufferedReader reader = getReaderForFile(INVALID_SEMANTIC_VERSIONS_CSV, getClass().getClassLoader())) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                assertNull(Semver.build(parts[0]));
            }
        }
    }

    @Test
    public void equalTo() throws IOException {
        try (BufferedReader reader = getReaderForFile(EQUAL_TO_SEMVER_CSV, getClass().getClassLoader())) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                Semver version1 = Semver.build(parts[0]);
                Semver version2 = Semver.build(parts[1]);

                boolean expectedResult = Boolean.parseBoolean(parts[2]);

                assertEquals(expectedResult, version1.equals(version2));
            }
        }
    }

    @Test
    public void between() throws IOException {
        try (BufferedReader reader = getReaderForFile(BETWEEN_SEMVER_CSV, getClass().getClassLoader())) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                Semver version1 = Semver.build(parts[0]);
                Semver version2 = Semver.build(parts[1]);
                Semver version3 = Semver.build(parts[2]);

                boolean result = version2.compare(version1) >= 0 && version2.compare(version3) <= 0;

                assertEquals(Boolean.parseBoolean(parts[3]), result);
            }
        }
    }

    @NonNull
    private static BufferedReader getReaderForFile(String fileName, @Nullable ClassLoader classLoader) throws FileNotFoundException {
        URL resource = classLoader.getResource(fileName);
        File file = new File(resource.getFile());
        return new BufferedReader(new FileReader(file));
    }
}
