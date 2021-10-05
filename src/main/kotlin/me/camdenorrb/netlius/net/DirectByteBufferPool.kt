package me.camdenorrb.netlius.net

import me.camdenorrb.netlius.Netlius
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentLinkedQueue

// TODO: Make a ByteBufferProvider api
class DirectByteBufferPool(size: Int, val bufferSize: Int = Netlius.DEFAULT_BUFFER_SIZE) {

    val byteBuffers = ConcurrentLinkedQueue(List(size) {
        ByteBuffer.allocateDirect(bufferSize)
    })


    inline fun take(size: Int, block: (ByteBuffer) -> Unit) {

        if (size > bufferSize) {

            if (DEBUG) {
                println("Allocated a new bytebuffer. Size: $size Count: ${byteBuffers.size}")
            }

            block(ByteBuffer.allocateDirect(size))
            return
        }

        var byteBuffer = byteBuffers.poll()?.limit(size)

        if (byteBuffer == null) {

            if (DEBUG) {
                println("Allocated a new bytebuffer. Size: $size Count: ${byteBuffers.size}")
            }

            byteBuffer = ByteBuffer.allocateDirect(size)!!
        }
        else {
            if (DEBUG) {
                println("Took bytebuffer. Size: $size Count: ${byteBuffers.size}")
            }
        }


        // Try + Finally so the buffer gets returned even with error
        try {
            block(byteBuffer)
        }
        finally {

            byteBuffer.clear()

            if (byteBuffers.size < size && byteBuffer.capacity() == bufferSize) {

                byteBuffers.add(byteBuffer)

                if (DEBUG) {
                    println("Added bytebuffer back. Count: ${byteBuffers.size}")
                }
            }
        }
    }

    companion object {

        const val DEBUG = false

    }

}
