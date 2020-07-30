package me.camdenorrb.netlius.net

import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentSkipListMap

object DirectByteBufferPool {

    // Sorted by lowest -> highest
    val byteBuffers = ConcurrentSkipListMap<Int, ConcurrentLinkedQueue<ByteBuffer>>()


    fun take(size: Int): ByteBuffer {
        return try {

            val entry = byteBuffers.ceilingEntry(size)

            if (entry.value.size == 1) {
                byteBuffers.remove(entry.key)
            }

            entry.value.remove().limit(size)
        }
        catch (ex: Exception) {

            when (ex) {

                is NullPointerException,
                is NoSuchElementException -> {
                    ByteBuffer.allocateDirect(size)
                }

                else -> throw ex
            }
        }
    }

    fun give(byteBuffer: ByteBuffer) {
        byteBuffer.clear()
        byteBuffers.getOrPut(byteBuffer.remaining(), { ConcurrentLinkedQueue() }).add(byteBuffer)
    }

}
