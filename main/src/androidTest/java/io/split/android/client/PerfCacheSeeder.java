package io.split.android.client;

import android.content.Context;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;

import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import helper.FileHelper;
import io.split.android.client.PerfSeedSpec.Spec;
import io.split.android.client.dtos.RuleBasedSegment;
import io.split.android.client.dtos.SegmentsChange;
import io.split.android.client.dtos.Split;
import io.split.android.client.main.BuildConfig;
import io.split.android.client.storage.cipher.SplitCipher;
import io.split.android.client.storage.cipher.SplitCipherFactory;
import io.split.android.client.storage.cipher.SplitEncryptionLevel;
import io.split.android.client.storage.db.GeneralInfoDao;
import io.split.android.client.storage.db.GeneralInfoEntity;
import io.split.android.client.storage.db.SplitEntity;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.StorageFactory;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentEntity;
import io.split.android.client.utils.Base64Util;
import io.split.android.client.utils.Json;

/**
 * Writes the cache a sync leaves behind (flags, rule-based segments, memberships and the general
 * info rows) for a {@link Spec}, and stores and restores a copy of the database file as a template.
 *
 * <p>It lives in the package of {@link SplitFactoryHelper} through {@link PerfSeedSpec}, which reaches
 * the package-private database naming.
 */
public final class PerfCacheSeeder {

    private static final String RBS_CHANGE_NUMBER = "rbsChangeNumber";
    private static final int INSERT_CHUNK = 500;
    private static final int FLAG_SET_COUNT = 10;
    // The default cache expiry is 10 days; a template older than this would be cleared on load.
    private static final long MAX_SEED_AGE_MS = TimeUnit.DAYS.toMillis(9);
    private static final String[] DATABASE_FILE_SUFFIXES = {"", "-wal", "-shm", "-journal"};

    private PerfCacheSeeder() {
    }

    /**
     * Writes the raw seed, replacing any database of that name, and returns the ciphertext bytes of
     * every flag and rule-based segment name and body (0 without encryption).
     */
    public static long writeRawSeed(Context ctx, String dbName, Spec spec) throws Exception {
        deleteDatabaseFiles(ctx, dbName);
        SplitCipher cipher = SplitCipherFactory.create(PerfSeedSpec.SDK_KEY, spec.encrypted);
        if (cipher == null) {
            throw new IllegalStateException("no cipher: the key store could not build one for the SDK key");
        }
        // Same settings as SplitRoomDatabase.getDatabase, but outside its static instance map, so the
        // factory under test still opens the file itself.
        SplitRoomDatabase db = Room.databaseBuilder(ctx.getApplicationContext(), SplitRoomDatabase.class, dbName)
                .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
                .fallbackToDestructiveMigration()
                .build();
        try {
            long now = System.currentTimeMillis();
            long ciphertextBytes = 0;

            List<JsonObject> templates = flagTemplates();
            Map<String, Integer> trafficTypes = new HashMap<>();
            Map<String, Set<String>> flagSets = new HashMap<>();
            List<SplitEntity> rows = new ArrayList<>();
            for (int i = 0; i < spec.flags; i++) {
                JsonObject template = templates.get(i % templates.size()).deepCopy();
                String name = PerfSeedSpec.flagName(i);
                template.addProperty("name", name);
                if (spec.flagSets) {
                    String set = "set_" + (i % FLAG_SET_COUNT);
                    JsonArray sets = new JsonArray();
                    sets.add(set);
                    template.add("sets", sets);
                    Set<String> members = flagSets.get(set);
                    if (members == null) {
                        members = new HashSet<>();
                        flagSets.put(set, members);
                    }
                    members.add(name);
                }
                Split split = Json.fromJson(template.toString(), Split.class);
                String trafficType = split.trafficTypeName.toLowerCase();
                Integer count = trafficTypes.get(trafficType);
                trafficTypes.put(trafficType, count == null ? 1 : count + 1);

                SplitEntity row = new SplitEntity();
                row.setName(encrypt(cipher, name));
                row.setBody(encrypt(cipher, Json.toJson(split)));
                row.setUpdatedAt(now / 1000);
                ciphertextBytes += cipherBytes(row.getName(), spec) + cipherBytes(row.getBody(), spec);
                rows.add(row);
            }
            // Serially through the dao: the storage's parallel transform can drop rows past its deadline.
            for (int from = 0; from < rows.size(); from += INSERT_CHUNK) {
                db.splitDao().insert(rows.subList(from, Math.min(from + INSERT_CHUNK, rows.size())));
            }

            JsonObject rbsTemplate = asset("split_changes_rbs.json").getAsJsonObject("rbs")
                    .getAsJsonArray("d").get(0).getAsJsonObject();
            List<RuleBasedSegmentEntity> rbsRows = new ArrayList<>();
            for (int j = 0; j < spec.rbs; j++) {
                JsonObject copy = rbsTemplate.deepCopy();
                String name = String.format(Locale.US, "rbs_%05d", j);
                copy.addProperty("name", name);
                RuleBasedSegment segment = Json.fromJson(copy.toString(), RuleBasedSegment.class);
                RuleBasedSegmentEntity row = new RuleBasedSegmentEntity(
                        encrypt(cipher, name), encrypt(cipher, Json.toJson(segment)), now);
                ciphertextBytes += cipherBytes(row.getName(), spec) + cipherBytes(row.getBody(), spec);
                rbsRows.add(row);
            }
            db.ruleBasedSegmentDao().insert(rbsRows);

            GeneralInfoDao info = db.generalInfoDao();
            info.update(new GeneralInfoEntity(GeneralInfoEntity.CHANGE_NUMBER_INFO, PerfSeedSpec.FLAGS_CN));
            info.update(new GeneralInfoEntity(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP, now));
            info.update(new GeneralInfoEntity(GeneralInfoEntity.FLAGS_SPEC, BuildConfig.FLAGS_SPEC));
            info.update(new GeneralInfoEntity(GeneralInfoEntity.TRAFFIC_TYPES_MAP,
                    encrypt(cipher, Json.toJson(trafficTypes))));
            // An empty value is what a cleared cache leaves; a sync with flag sets writes the encrypted map.
            info.update(new GeneralInfoEntity(GeneralInfoEntity.FLAG_SETS_MAP,
                    spec.flagSets ? encrypt(cipher, Json.toJson(flagSets)) : ""));
            info.update(new GeneralInfoEntity(GeneralInfoEntity.DATABASE_ENCRYPTION_MODE,
                    (spec.encrypted ? SplitEncryptionLevel.AES_128_CBC : SplitEncryptionLevel.NONE).toString()));
            StorageFactory.getGeneralInfoStorage(db, null).setRbsChangeNumber(PerfSeedSpec.RBS_CN);
            StorageFactory.getMySegmentsStorage(db, cipher).getStorageForKey(PerfSeedSpec.USER_KEY)
                    .set(SegmentsChange.create(new HashSet<>(Arrays.asList(PerfSeedSpec.SEGMENTS)), PerfSeedSpec.MEMBERSHIPS_CN));
            StorageFactory.getMyLargeSegmentsStorage(db, cipher).getStorageForKey(PerfSeedSpec.USER_KEY)
                    .set(SegmentsChange.create(new HashSet<String>(), PerfSeedSpec.MEMBERSHIPS_CN));
            return ciphertextBytes;
        } finally {
            db.close();
        }
    }

    /**
     * Checks that the database on disk is a complete seed and stores a copy as the template for the
     * spec. {@code startMs} is a time taken before the seed was written (or before whatever last
     * touched the database), so a stale timestamp row is caught.
     */
    public static void storeTemplate(Context ctx, String dbName, Spec spec, long startMs,
                                     long ciphertextBytes) throws Exception {
        File db = ctx.getDatabasePath(dbName);
        SQLiteDatabase check = SQLiteDatabase.openDatabase(db.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
        try {
            // A dropped write, a cleared cache or an encrypt that returned null would otherwise
            // measure an empty cache, which looks fast.
            PerfSeedSpec.requireCount("splits", DatabaseUtils.queryNumEntries(check, "splits"), spec.flags);
            PerfSeedSpec.requireCount("rule_based_segments",
                    DatabaseUtils.queryNumEntries(check, "rule_based_segments"), spec.rbs);
            PerfSeedSpec.requireCount("my_segments", DatabaseUtils.queryNumEntries(check, "my_segments"), 1);
            long stamped = longValue(check, GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP);
            if (stamped < startMs) {
                throw new IllegalStateException(GeneralInfoEntity.SPLITS_UPDATE_TIMESTAMP + " is " + stamped
                        + ", before the start " + startMs + ": the write hadn't landed before the copy");
            }
            // A refused write leaves -1 here, and every launch would then do a full fetch.
            PerfSeedSpec.requireLong(GeneralInfoEntity.CHANGE_NUMBER_INFO,
                    longValue(check, GeneralInfoEntity.CHANGE_NUMBER_INFO), PerfSeedSpec.FLAGS_CN);
            PerfSeedSpec.requireLong(RBS_CHANGE_NUMBER, longValue(check, RBS_CHANGE_NUMBER), PerfSeedSpec.RBS_CN);
        } finally {
            check.close();
        }
        File dir = templateDir(ctx, spec);
        dir.mkdirs();
        File[] old = dir.listFiles();
        if (old != null) {
            for (File file : old) {
                file.delete();
            }
        }
        copy(db, new File(dir, "db"));
        File wal = new File(db.getPath() + "-wal");
        if (wal.exists()) {
            copy(wal, new File(dir, "db-wal"));
        }
        JSONObject manifest = new JSONObject()
                .put("seed", spec.id())
                .put("seededAtMs", System.currentTimeMillis())
                .put("flags", spec.flags)
                .put("rbs", spec.rbs)
                .put("sdk", BuildConfig.SPLIT_VERSION_NAME)
                .put("flagsSpec", BuildConfig.FLAGS_SPEC)
                .put("fingerprint", Build.FINGERPRINT)
                .put("apkUpdatedAtMs", apkUpdatedAt(ctx))
                .put("ciphertextBytes", ciphertextBytes);
        write(new File(dir, "manifest.json"), manifest.toString(2));
    }

    /** Copies the template over the database files. File copies only: no SDK class touches the database. */
    public static void restoreTemplate(Context ctx, String dbName, Spec spec) throws Exception {
        JSONObject m = manifest(ctx, spec);
        if (System.currentTimeMillis() - m.getLong("seededAtMs") > MAX_SEED_AGE_MS) {
            throw new IllegalStateException("seed older than 9 days; the expiry would clear it: seed again");
        }
        if (!Build.FINGERPRINT.equals(m.getString("fingerprint"))) {
            throw new IllegalStateException("the OS changed since seeding: seed again");
        }
        if (m.getLong("apkUpdatedAtMs") != apkUpdatedAt(ctx)) {
            // The template was made by other code, so it isn't what this build leaves behind.
            throw new IllegalStateException("the APK was reinstalled since seeding: seed again");
        }
        File dir = templateDir(ctx, spec);
        File db = ctx.getDatabasePath(dbName);
        deleteDatabaseFiles(ctx, dbName);
        copy(new File(dir, "db"), db);
        File wal = new File(dir, "db-wal");
        if (wal.exists()) {
            copy(wal, new File(db.getPath() + "-wal"));
        }
    }

    /** The stored manifest; a FileNotFoundException means no template was stored for the spec. */
    public static JSONObject manifest(Context ctx, Spec spec) throws IOException, JSONException {
        return new JSONObject(read(new File(templateDir(ctx, spec), "manifest.json")));
    }

    private static void deleteDatabaseFiles(Context ctx, String dbName) {
        String path = ctx.getDatabasePath(dbName).getPath();
        for (String suffix : DATABASE_FILE_SUFFIXES) {
            new File(path + suffix).delete();
        }
    }

    /** Row counts read from the database on disk, for a status line. */
    public static String describeRows(Context ctx, String dbName) {
        SQLiteDatabase db = SQLiteDatabase.openDatabase(ctx.getDatabasePath(dbName).getPath(), null,
                SQLiteDatabase.OPEN_READONLY);
        try {
            return "splits=" + DatabaseUtils.queryNumEntries(db, "splits")
                    + " rule_based_segments=" + DatabaseUtils.queryNumEntries(db, "rule_based_segments")
                    + " my_segments=" + DatabaseUtils.queryNumEntries(db, "my_segments");
        } finally {
            db.close();
        }
    }

    private static List<JsonObject> flagTemplates() {
        List<JsonObject> templates = new ArrayList<>();
        for (JsonElement e : asset("split_changes_1.json").getAsJsonObject("ff").getAsJsonArray("splits")) {
            JsonArray conditions = e.getAsJsonObject().getAsJsonArray("conditions");
            // The flag without conditions is left out: it would understate decrypt and parse cost.
            if (conditions != null && conditions.size() > 0) {
                templates.add(e.getAsJsonObject());
            }
        }
        return templates;
    }

    private static String encrypt(SplitCipher cipher, String plain) {
        String value = cipher.encrypt(plain);
        if (value == null) {
            throw new IllegalStateException("encrypt returned null");
        }
        return value;
    }

    private static long cipherBytes(String stored, Spec spec) {
        if (!spec.encrypted) {
            return 0;
        }
        byte[] raw = Base64Util.bytesDecode(stored);
        return raw == null ? 0 : raw.length;
    }

    private static long longValue(SQLiteDatabase db, String name) {
        return DatabaseUtils.longForQuery(db, "SELECT longValue FROM general_info WHERE name = ?",
                new String[]{name});
    }

    private static JsonObject asset(String name) {
        String json = new FileHelper().loadFileContent(InstrumentationRegistry.getInstrumentation().getContext(), name);
        return JsonParser.parseString(json).getAsJsonObject();
    }

    private static long apkUpdatedAt(Context ctx) throws PackageManager.NameNotFoundException {
        return ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).lastUpdateTime;
    }

    private static File templateDir(Context ctx, Spec spec) {
        return new File(ctx.getFilesDir(), "perf-seeds/" + spec.id());
    }

    private static void copy(File from, File to) throws IOException {
        File parent = to.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        try (InputStream in = new FileInputStream(from); OutputStream out = new FileOutputStream(to)) {
            byte[] buffer = new byte[64 * 1024];
            for (int n; (n = in.read(buffer)) > 0; ) {
                out.write(buffer, 0, n);
            }
        }
    }

    private static String read(File f) throws IOException {
        byte[] bytes = new byte[(int) f.length()];
        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            in.readFully(bytes);
        }
        return new String(bytes, "UTF-8");
    }

    private static void write(File f, String text) throws IOException {
        try (OutputStream out = new FileOutputStream(f)) {
            out.write(text.getBytes("UTF-8"));
        }
    }
}
