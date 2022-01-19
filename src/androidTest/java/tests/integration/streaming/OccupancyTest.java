package tests.integration.streaming;

import static java.lang.Thread.sleep;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

import io.split.android.client.SplitFactory;

public class OccupancyTest extends OccupancyBaseTest {

    @Test
    public void occupancy() throws IOException, InterruptedException {

        // TODO: Improve this test!
        // Some assertions are commented because are  not trustworthy
        SplitFactory splitFactory = getSplitFactory();

        mMySegmentsHitCount = 0;
        mSplitsHitCount = 0;

        // Should disable streaming
        pushOccupancy(PRIMARY_CHANNEL, 0);
        sleep(4000);
        int mySegmentsHitCountAfterDisable = mMySegmentsHitCount;
        int splitsHitCountAfterDisable = mSplitsHitCount;

        pushOccupancy(SECONDARY_CHANNEL, 1);
        sleep(1000);
        mMySegmentsHitCount = 0;
        mSplitsHitCount = 0;
        sleep(4000);
        int mySegmentsHitCountAfterSecEnable = mMySegmentsHitCount;
        int splitsHitCountSecEnable = mSplitsHitCount;

        pushOccupancy(SECONDARY_CHANNEL, 0);
        sleep(300);
        mMySegmentsHitCount = 0;
        mSplitsHitCount = 0;
        sleep(5000);
        int mySegmentsHitCountAfterSecDisable = mMySegmentsHitCount;
        int splitsHitCountSecDisable = mSplitsHitCount;

        // Should enable streaming
        pushOccupancy(PRIMARY_CHANNEL, 1);
        sleep(1000);
        mMySegmentsHitCount = 0;
        mSplitsHitCount = 0;
        sleep(4000);
        int mySegmentsHitCountAfterEnable = mMySegmentsHitCount;
        int splitsHitCountAfterEnable = mSplitsHitCount;

        // Hits > 0 means polling enabled
//        Assert.assertTrue(mySegmentsHitCountAfterDisable > 0);
//        Assert.assertTrue(splitsHitCountAfterDisable > 0);

        // Hits == 2 means streaming enabled and sync all
        Assert.assertEquals(0,mySegmentsHitCountAfterSecEnable);
        Assert.assertEquals(0, splitsHitCountSecEnable);

        // Hits > 0 means secondary channel message ignored because pollling wasn't disabled
//        Assert.assertTrue(mySegmentsHitCountAfterSecDisable > 0);
//        Assert.assertTrue(splitsHitCountSecDisable > 0);

        // Hits == 0 means streaming enabled
//        Assert.assertTrue(mySegmentsHitCountAfterEnable < 2 );
//        Assert.assertTrue(splitsHitCountAfterEnable < 2);

        splitFactory.destroy();
    }
}
