package me.camdenorrb.netlius.net

import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedDeque

class DirectByteBufferPool(size: Int, val bufferSize: Int = DEFAULT_BUFFER_SIZE) {

    val byteBuffers = ConcurrentLinkedDeque(MutableList(size) {
        ByteBuffer.allocateDirect(bufferSize)
    })


    inline fun take(size: Int = DEFAULT_BUFFER_SIZE, block: (ByteBuffer) -> Unit) {

        val byteBuffer = if (size > bufferSize) {
            ByteBuffer.allocateDirect(size)
        }
        else {
            byteBuffers.poll() ?: ByteBuffer.allocateDirect(size)
        }

        byteBuffer.limit(size)
        block(byteBuffer)
        byteBuffer.clear()

        if (byteBuffers.size < size && byteBuffer.capacity() == bufferSize) {
            byteBuffers.push(byteBuffer)
        }
    }


    companion object {

        const val DEFAULT_BUFFER_SIZE = 8_192

    }

}
