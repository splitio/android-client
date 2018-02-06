package io.split.android.engine;

import timber.log.Timber;


import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SDKReadinessGates {

    private final CountDownLatch _splitsAreReady = new CountDownLatch(1);
    private final CountDownLatch _mySegmentsAreReady = new CountDownLatch(1);


    /**
     * Returns true if the SDK is ready. The SDK is ready when:
     * <ol>
     * <li>It has fetched Split definitions the first time.</li>
     * <li>It has downloaded segment memberships for segments in use in the initial split definitions</li>
     * </ol>
     * <p/>
     * This operation will block until the SDK is ready or 'milliseconds' have passed. If the milliseconds
     * are less than or equal to zero, the operation will not block and return immediately
     *
     * @param milliseconds time to wait for an answer. if the value is zero or negative, we will not
     *                     block for an answer.
     * @return true if the sdk is ready, false otherwise.
     * @throws InterruptedException if this operation was interrupted.
     */
    public boolean isSDKReady(long milliseconds) throws InterruptedException {
        long end = System.currentTimeMillis() + milliseconds;
        long timeLeft = milliseconds;

        boolean splits = areSplitsReady(timeLeft);
        if (!splits) {
            return false;
        }

        timeLeft = end - System.currentTimeMillis();

        return areMySegmentsReady(timeLeft);

    }

    /**
     * Records that the SDK split initialization is done.
     * This operation is atomic and idempotent. Repeated invocations
     * will not have any impact on the state.
     */
    public void splitsAreReady() {
        long originalCount = _splitsAreReady.getCount();
        _splitsAreReady.countDown();
        if (originalCount > 0L) {
            Timber.i("splits are ready");
        }
    }

    /**
     * Records that the SDK mySegments initialization is done.
     * This operation is atomic and idempotent. Repeated invocations
     * will not have any impact on the state.
     */
    public void mySegmentsAreReady() {
        long originalCount = _mySegmentsAreReady.getCount();
        _mySegmentsAreReady.countDown();
        if (originalCount > 0L) {
            Timber.i("mySegments are ready");
        }
    }

    /**
     * Returns true if the SDK is ready w.r.t mySegments. In other words, this method returns true if:
     * <ol>
     * <li>The SDK has fetched mySegments the first time.</li>
     * </ol>
     * <p/>
     * This operation will block until the SDK is ready or 'milliseconds' have passed. If the milliseconds
     * are less than or equal to zero, the operation will not block and return immediately
     *
     * @param milliseconds time to wait for an answer. if the value is zero or negative, we will not
     *                     block for an answer.
     * @return true if the sdk is ready w.r.t splits, false otherwise.
     * @throws InterruptedException if this operation was interrupted.
     */
    public boolean areMySegmentsReady(long milliseconds) throws InterruptedException {
        return _mySegmentsAreReady.await(milliseconds, TimeUnit.MILLISECONDS);
    }

    /**
     * Returns true if the SDK is ready w.r.t splits. In other words, this method returns true if:
     * <ol>
     * <li>The SDK has fetched Split definitions the first time.</li>
     * </ol>
     * <p/>
     * This operation will block until the SDK is ready or 'milliseconds' have passed. If the milliseconds
     * are less than or equal to zero, the operation will not block and return immediately
     *
     * @param milliseconds time to wait for an answer. if the value is zero or negative, we will not
     *                     block for an answer.
     * @return true if the sdk is ready w.r.t splits, false otherwise.
     * @throws InterruptedException if this operation was interrupted.
     */
    public boolean areSplitsReady(long milliseconds) throws InterruptedException {
        return _splitsAreReady.await(milliseconds, TimeUnit.MILLISECONDS);
    }


    public boolean registerSegments(Collection<String> segmentNames) throws InterruptedException {
        return true;
    }

    public void segmentIsReady(String segmentName) {

    }

    public boolean isSegmentRegistered(String segmentName) {
        return true;
    }
}