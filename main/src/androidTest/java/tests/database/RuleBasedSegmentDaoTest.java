package tests.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.test.core.app.ApplicationProvider;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import helper.DatabaseHelper;
import io.split.android.client.storage.db.SplitRoomDatabase;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentDao;
import io.split.android.client.storage.db.rbs.RuleBasedSegmentEntity;

public class RuleBasedSegmentDaoTest {

    private SplitRoomDatabase database;
    private RuleBasedSegmentDao dao;

    @Before
    public void setUp() {
        database = DatabaseHelper.getTestDatabase(ApplicationProvider.getApplicationContext());
        dao = database.ruleBasedSegmentDao();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void insertAndGetAll() {
        RuleBasedSegmentEntity entity1 = new RuleBasedSegmentEntity("segment1", "body1", getUpdatedAt());
        RuleBasedSegmentEntity entity2 = new RuleBasedSegmentEntity("segment2", "body2", getUpdatedAt());

        dao.insert(Arrays.asList(entity1, entity2));

        List<RuleBasedSegmentEntity> allSegments = dao.getAll();
        assertEquals(2, allSegments.size());
        assertTrue(allSegments.stream().anyMatch(e -> e.getName().equals("segment1")));
        assertTrue(allSegments.stream().anyMatch(e -> e.getName().equals("segment2")));
    }

    @Test
    public void insertSingleEntity() {
        RuleBasedSegmentEntity entity = new RuleBasedSegmentEntity("segment3", "body3", getUpdatedAt());
        dao.insert(entity);

        List<RuleBasedSegmentEntity> allSegments = dao.getAll();
        assertEquals(1, allSegments.size());
        assertEquals("segment3", allSegments.get(0).getName());
    }

    @Test
    public void updateEntity() {
        RuleBasedSegmentEntity entity = new RuleBasedSegmentEntity("segment4", "body4", getUpdatedAt());
        dao.insert(entity);

        dao.update("segment4", "newSegment4", "newBody4");

        List<RuleBasedSegmentEntity> allSegments = dao.getAll();
        assertEquals(1, allSegments.size());
        assertEquals("newSegment4", allSegments.get(0).getName());
        assertEquals("newBody4", allSegments.get(0).getBody());
    }

    @Test
    public void deleteEntities() {
        RuleBasedSegmentEntity entity1 = new RuleBasedSegmentEntity("segment5", "body5", getUpdatedAt());
        RuleBasedSegmentEntity entity2 = new RuleBasedSegmentEntity("segment6", "body6", getUpdatedAt());
        dao.insert(Arrays.asList(entity1, entity2));

        dao.delete(Arrays.asList("segment5"));

        List<RuleBasedSegmentEntity> allSegments = dao.getAll();
        assertEquals(1, allSegments.size());
        assertEquals("segment6", allSegments.get(0).getName());
    }

    @Test
    public void deleteAll() {
        RuleBasedSegmentEntity entity1 = new RuleBasedSegmentEntity("segment7", "body7", getUpdatedAt());
        RuleBasedSegmentEntity entity2 = new RuleBasedSegmentEntity("segment8", "body8", getUpdatedAt());
        dao.insert(Arrays.asList(entity1, entity2));

        dao.deleteAll();

        List<RuleBasedSegmentEntity> allSegments = dao.getAll();
        assertTrue(allSegments.isEmpty());
    }

    private static long getUpdatedAt() {
        return System.currentTimeMillis();
    }
}
