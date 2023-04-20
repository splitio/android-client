package io.split.android.client.storage.cipher

import org.junit.Test
import kotlin.test.assertNotNull

class ObjectPoolTest {

    @Test
    fun testInstancesAreDifferent() {
        val objectPool = ObjectPool(3) { "ABCDEFGHIJKLMNOPQRSTUVWXYZ".random() }

        val instance1 = objectPool.acquire()
        val instance2 = objectPool.acquire()
        val instance3 = objectPool.acquire()

        objectPool.release(instance1)
        objectPool.release(instance2)
        objectPool.release(instance3)

        val instance4 = objectPool.acquire()
        objectPool.release(instance4)

        val instance5 = objectPool.acquire()
        objectPool.release(instance5)

        val instance6 = objectPool.acquire()
        objectPool.release(instance6)

        val list = mutableListOf<Char>(
            instance1,
            instance2,
            instance3,
            instance4,
            instance5,
            instance6
        )

        assert(list.distinct().size == 3)
    }

    @Test
    fun acquireDoesNotReturnNullInstances() {
        val objectPool = ObjectPool(3) { "ABCDEFGHIJKLMNOPQRSTUVWXYZ".random() }

        val instance1 = objectPool.acquire()
        val instance2 = objectPool.acquire()
        val instance3 = objectPool.acquire()
        val instance4 = objectPool.acquire()

        assertNotNull(instance4)
    }
}
