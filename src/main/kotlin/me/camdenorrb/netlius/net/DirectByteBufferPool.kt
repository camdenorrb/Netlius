package me.camdenorrb.netlius.net

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.count
import me.camdenorrb.netlius.Netlius
import java.nio.ByteBuffer
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

object DirectByteBufferPool {

    const val BUFFER_COUNT = 20

    const val BUFFER_SIZE  = 65_536


    val byteBuffers = Channel<ByteBuffer>(Channel.Factory.UNLIMITED)


    init {
        CoroutineScope.launch {
            repeat(BUFFER_COUNT) {
                byteBuffers.send(ByteBuffer.allocateDirect(BUFFER_SIZE))
            }
        }

    }


    suspend fun take(size: Int): ByteBuffer {

        check (size <= BUFFER_SIZE) {
            "ByteBuffer size too much, $size > $BUFFER_SIZE"
        }

        return byteBuffers.receive().limit(size)
    }

    fun give(byteBuffer: ByteBuffer) {

        byteBuffer.clear()

        CoroutineScope.launch {
            byteBuffers.send(byteBuffer)
        }
    }

    object CoroutineScope : kotlinx.coroutines.CoroutineScope {

        override val coroutineContext = Netlius.cachedThreadPoolDispatcher

    }

}
