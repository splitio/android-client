package tests.integration.userconsent

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import fake.*
import helper.*
import io.split.android.client.SplitClient
import io.split.android.client.SplitFactory
import io.split.android.client.dtos.SplitChange
import io.split.android.client.events.SplitEvent
import io.split.android.client.events.SplitEventTask
import io.split.android.client.network.HttpMethod
import io.split.android.client.service.impressions.ImpressionsMode
import io.split.android.client.shared.UserConsent
import io.split.android.client.storage.db.ImpressionDao
import io.split.android.client.storage.db.ImpressionsCountDao
import io.split.android.client.storage.db.StorageRecordStatus
import io.split.android.client.utils.Json
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.lang.Thread.sleep
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class UserConsentModeOptimizedTest {

    private var mImpLatch: CountDownLatch? = null
    private var mCountLatch: CountDownLatch? = null
    private lateinit var mContext: Context
    private lateinit var mImpDao: ImpressionDao
    private lateinit var mCountDao: ImpressionsCountDao
    private val mLifecycleManager = LifecycleManagerStub()

    var mImpPosted = false
    var mCountPosted = false

    @Before
    fun setUp() {
        mContext = InstrumentationRegistry.getInstrumentation().context
        mImpPosted = false
        mCountPosted = false
        mCountLatch = null
        mImpLatch = null
    }

    @Test
    fun testUserConsentGranted() {
        val factory = createFactory(UserConsent.GRANTED)
        val readyExp = CountDownLatch(1)
        mImpLatch = CountDownLatch(1)
        mCountLatch = CountDownLatch(1)

        val client = factory.client()
        client.on(SplitEvent.SDK_READY, object: SplitEventTask(){
            override fun onPostExecution(client: SplitClient?) {}

            override fun onPostExecutionView(client: SplitClient?) = readyExp.countDown()
        })

        readyExp.await(5, TimeUnit.SECONDS)

        for (i in 1..20) {
            client.getTreatment("FACUNDO_TEST")
            client.track("ev", i.toDouble())
        }
        sleep(150)
        mLifecycleManager.simulateOnPause()
        mLifecycleManager.simulateOnResume()
        mImpLatch?.await(20, TimeUnit.SECONDS)
        mCountLatch?.await(20, TimeUnit.SECONDS)

        Assert.assertTrue(mImpPosted)
        Assert.assertTrue(mCountPosted)
    }

    @Test
    fun userConsentDeclined() {
        val factory = createFactory(UserConsent.DECLINED)
        val readyExp = CountDownLatch(1)

        val client = factory.client()
        client.on(SplitEvent.SDK_READY, object: SplitEventTask(){
            override fun onPostExecution(client: SplitClient?) {}

            override fun onPostExecutionView(client: SplitClient?) = readyExp.countDown()
        })

        readyExp.await(5, TimeUnit.SECONDS)

        for (i in 1..20) {
            client.getTreatment("FACUNDO_TEST")
        }
        mLifecycleManager.simulateOnPause()
        mLifecycleManager.simulateOnResume()
        sleep(2000)
        client.flush()
        sleep(2000)
        val imp = mImpDao.getBy(-1, StorageRecordStatus.ACTIVE, 100)
        val count = mCountDao.getBy(-1, StorageRecordStatus.ACTIVE, 100)

        Assert.assertFalse(mImpPosted)
        Assert.assertFalse(mCountPosted)
        Assert.assertEquals(0, imp.count())
        Assert.assertEquals(0, count.count())
    }

    @Test
    fun userConsentUnknownThenGranted() {
        val factory = createFactory(UserConsent.UNKNOWN)
        val readyExp = CountDownLatch(1)
        mImpLatch = CountDownLatch(1)
        mCountLatch = CountDownLatch(1)

        val client = factory.client()
        client.on(SplitEvent.SDK_READY, object: SplitEventTask(){
            override fun onPostExecution(client: SplitClient?) {}
            override fun onPostExecutionView(client: SplitClient?) = readyExp.countDown()
        })

        readyExp.await(5, TimeUnit.SECONDS)

        for (i in 1..20) {
            client.getTreatment("FACUNDO_TEST")
        }

        mLifecycleManager.simulateOnPause()
        mLifecycleManager.simulateOnResume()
        sleep(2000)
        client.flush()
        sleep(2000)
        val impPostedBefore = mImpPosted
        val countPostedBefore = mCountPosted

        mImpPosted = false
        mCountPosted = false

        factory.setUserConsent(true)
        sleep(2000)
        client.flush()
        mImpLatch?.await(10, TimeUnit.SECONDS)
        mCountLatch?.await(10, TimeUnit.SECONDS)

        val impPostedAfter = mImpPosted
        val countPostedAfter = mCountPosted

        Assert.assertFalse(impPostedBefore)
        Assert.assertFalse(countPostedBefore)
        Assert.assertTrue(impPostedAfter)
        Assert.assertTrue(countPostedAfter)
    }

    @Test
    fun userConsentUnknownThenDeclined() {
        val factory = createFactory(UserConsent.UNKNOWN)
        val readyExp = CountDownLatch(1)
        mImpLatch = CountDownLatch(1)
        mCountLatch = CountDownLatch(1)

        val client = factory.client()
        client.on(SplitEvent.SDK_READY, object: SplitEventTask(){
            override fun onPostExecution(client: SplitClient?) {}
            override fun onPostExecutionView(client: SplitClient?) = readyExp.countDown()
        })

        readyExp.await(5, TimeUnit.SECONDS)

        for (i in 1..20) {
            client.getTreatment("FACUNDO_TEST")
        }

        mLifecycleManager.simulateOnPause()
        mLifecycleManager.simulateOnResume()
        sleep(2000)
        client.flush()
        sleep(2000)
        val impPostedBefore = mImpPosted
        val countPostedBefore = mCountPosted

        mImpPosted = false
        mCountPosted = false

        factory.setUserConsent(false)
        mLifecycleManager.simulateOnPause()
        mLifecycleManager.simulateOnResume()
        sleep(2000)
        client.flush()
        sleep(2000)

        val impPostedAfter = mImpPosted
        val countPostedAfter = mCountPosted

        val imp = mImpDao.getBy(-1, StorageRecordStatus.ACTIVE, 100)
        val count = mCountDao.getBy(-1, StorageRecordStatus.ACTIVE, 100)

        Assert.assertFalse(impPostedBefore)
        Assert.assertFalse(countPostedBefore)
        Assert.assertFalse(impPostedAfter)
        Assert.assertFalse(countPostedAfter)
        Assert.assertEquals(0, imp.count())
        Assert.assertEquals(0, count.count())
    }

    private fun createFactory(userConsent: UserConsent): SplitFactory {
        val splitRoomDatabase = DatabaseHelper.getTestDatabase(mContext)
        splitRoomDatabase.clearAllTables()
        mImpDao = splitRoomDatabase.impressionDao()
        mCountDao = splitRoomDatabase.impressionsCountDao()
        val dispatcher: HttpResponseMockDispatcher = buildDispatcher()
        val config = TestableSplitConfigBuilder().ready(30000)
            .trafficType("account")
            .impressionsMode(ImpressionsMode.OPTIMIZED)
            .impressionsRefreshRate(3)
            .impressionsCountersRefreshRate(3)
            .mtkRefreshRate(3)
            .enableDebug()
            .eventFlushInterval(3)
            .userConsent(userConsent)
            .build()

        return IntegrationHelper.buildFactory(
            IntegrationHelper.dummyApiKey(),
            IntegrationHelper.dummyUserKey(),
            config,
            mContext,
            HttpClientMock(dispatcher),
            splitRoomDatabase, null, null,
            mLifecycleManager
        )
    }

    var mChangeHit = 0
    private fun buildDispatcher(): HttpResponseMockDispatcher {
        return object : HttpResponseMockDispatcher {
            override fun getStreamResponse(uri: URI): HttpStreamResponseMock {
                return HttpStreamResponseMock(200, null)
            }

            override fun getResponse(uri: URI, method: HttpMethod, body: String?): HttpResponseMock {
                println(uri.path)
                return if (uri.path.contains("/" + IntegrationHelper.ServicePath.MEMBERSHIPS)) {
                    HttpResponseMock(200, IntegrationHelper.emptyAllSegments())
                } else if (uri.path.contains("/splitChanges")) {
                    if (mChangeHit == 0) {
                        mChangeHit+=1
                        return getSplitsMockResponse("")
                    }
                    return HttpResponseMock(200, IntegrationHelper.emptySplitChanges(99999999, 99999999))
                } else if (uri.path.contains("/testImpressions/bulk")) {
                    if (!mImpPosted) {
                        mImpPosted = true
                        mImpLatch?.countDown()
                    }
                    HttpResponseMock(200)
                } else if (uri.path.contains("/testImpressions/count")) {
                    if (!mCountPosted) {
                        mCountPosted = true
                        mCountLatch?.countDown()
                    }
                    HttpResponseMock(200)
                } else if (uri.path.contains("/events/bulk")) {
                    HttpResponseMock(200)
                } else if (uri.path.contains("/keys/cs")) {
                    HttpResponseMock(200)
                } else if (uri.path.contains("/auth")) {
                    HttpResponseMock(200, IntegrationHelper.streamingEnabledToken())
                } else {
                    HttpResponseMock(404)
                }
            }
        }
    }

    private fun getSplitsMockResponse(since: String): HttpResponseMock {
        return HttpResponseMock(200, loadSplitChanges())
    }

    private fun loadSplitChanges(): String? {
        val fileHelper = FileHelper()
        val change = fileHelper.loadFileContent(mContext, "split_changes_1.json")
        val parsedChange = Json.fromJson(change, SplitChange::class.java)
        parsedChange.since = parsedChange.till
        return Json.toJson(parsedChange)
    }
}
