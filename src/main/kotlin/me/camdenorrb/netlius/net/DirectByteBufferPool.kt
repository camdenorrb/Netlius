package me.camdenorrb.netlius.net

/*
import me.camdenorrb.netlius.Netlius
import java.io.EOFException
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue

class DirectByteBufferPool(size: Int, val bufferSize: Int = Netlius.DEFAULT_BUFFER_SIZE) {

    val byteBuffers = ConcurrentLinkedQueue(List(size) {
        ByteBuffer.allocateDirect(bufferSize)
    })


    inline fun take(size: Int, block: (ByteBuffer) -> Unit) {

        val byteBuffer = if (size > bufferSize) {
            ByteBuffer.allocateDirect(size)
        }
        else {
            byteBuffers.poll()?.limit(size) ?: ByteBuffer.allocateDirect(size)
        }

        // Try + Finally so the buffer gets returned even with error
        try {
            block(byteBuffer)
        }
        finally {

            byteBuffer.clear()

            if (byteBuffers.size < size && byteBuffer.capacity() == bufferSize) {
                byteBuffers.add(byteBuffer)
            }
        }
    }

}
*/