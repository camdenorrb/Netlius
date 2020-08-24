package me.camdenorrb.netlius.net

import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue

class DirectByteBufferPool(size: Int, val bufferSize: Int = DEFAULT_BUFFER_SIZE) {

    val byteBuffers = ConcurrentLinkedQueue(List(size) {
        ByteBuffer.allocateDirect(bufferSize)
    })


    inline fun take(size: Int = DEFAULT_BUFFER_SIZE, block: (ByteBuffer) -> Unit) {

        val byteBuffer = if (size > bufferSize) {
            ByteBuffer.allocateDirect(size)
        }
        else {
            byteBuffers.poll() ?: ByteBuffer.allocateDirect(size)
        }

        // Try + Finally so the buffer gets returned even with error
        try {
            block(byteBuffer.limit(size))
        }
        finally {

            byteBuffer.clear()

            if (byteBuffers.size < size && byteBuffer.capacity() == bufferSize) {
                byteBuffers.add(byteBuffer)
            }
        }
    }

}
