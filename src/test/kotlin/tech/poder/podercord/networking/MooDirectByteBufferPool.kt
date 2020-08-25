package tech.poder.podercord.networking

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.ByteBuffer

class MooDirectByteBufferPool(size: Int, val minBufferSize: Int = DEFAULT_BUFFER_SIZE) {

    val byteBuffers = ArrayDeque<ByteBuffer>(List(size) {
        ByteBuffer.allocateDirect(minBufferSize)
    })

    val lock = Mutex(false)

    suspend inline fun take(size: Int = DEFAULT_BUFFER_SIZE, block: (ByteBuffer) -> Unit) {
        val byteBuffer = lock.withLock {
            if (byteBuffers.isEmpty()) {
                /*
                byteBuffers.addAll(List(10) {
                    ByteBuffer.allocateDirect(maxOf(size, minBufferSize))
                })
                */
                //byteBuffers.removeLast()
                ByteBuffer.allocateDirect(maxOf(size, minBufferSize))
            } else {
                var tmp = byteBuffers.removeLast()
                if (tmp.remaining() < size) {
                    tmp = ByteBuffer.allocateDirect(size)
                } else {
                    tmp.limit(size)
                }
                tmp
            }
        }


        try {
            block(byteBuffer)
        } finally {
            if (byteBuffer.capacity() == minBufferSize) {
                byteBuffer.clear()
                lock.withLock {
                    byteBuffers.addLast(byteBuffer)
                }
            }
        }
    }
}