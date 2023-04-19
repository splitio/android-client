package tests.database

import androidx.test.platform.app.InstrumentationRegistry
import io.split.android.client.storage.db.SplitEntity
import io.split.android.client.storage.db.SplitRoomDatabase
import io.split.android.client.storage.db.attributes.AttributesEntity
import org.junit.Before
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.fail

class ExclusiveTransactionTest {

    private lateinit var db: SplitRoomDatabase

    @Before
    fun setUp() {
        db = SplitRoomDatabase.getDatabase(
            InstrumentationRegistry.getInstrumentation().context,
            "test_db"
        )
    }

    @Test
    fun transactionPreventsReads() {
        val writeFinished = AtomicBoolean(false)
        val readFinished = AtomicBoolean(false)

        // Insert multiple values in multiple DAOs inside a transaction
        val updateThread = Thread() {
            db.runInTransaction {
                for (i in 0..400) {
                    db.splitDao().insert(
                        SplitEntity().apply {
                            name = "split${i}"
                            body = "body${i}"
                            updatedAt = System.currentTimeMillis()
                        }
                    )

                    db.attributesDao().update(
                        AttributesEntity().apply {
                            userKey = "key${i}"
                            attributes = "value${i}"
                        }
                    )
                }
                writeFinished.set(true)
            }
        }

        // Read values from multiple DAOs
        val readThread = Thread() {
            db.splitDao().all
            db.attributesDao().all
            readFinished.set(true)
        }

        // Monitor the operations
        val monitorThread = Thread() {
            while (true) {
                if (readFinished.get() && !writeFinished.get()) {
                    fail("Values were read before update was done")
                } else if (writeFinished.get() && readFinished.get()) {
                    break
                }
            }
        }

        monitorThread.start()
        readThread.start()
        updateThread.start()

        monitorThread.join(2000)
        readThread.join(2000)
        updateThread.join(2000)
    }
}
