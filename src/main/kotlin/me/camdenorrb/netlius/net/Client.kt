package me.camdenorrb.netlius.net

import me.camdenorrb.netlius.Netlius
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.*

// https://www.baeldung.com/java-nio2-async-socket-channel
// TODO: Use a different coroutine for writing and reading
class Client internal constructor(val channel: AsynchronousSocketChannel) {

    val packetQueue = mutableListOf<Packet>()


    init {

        Netlius.running++

        channel.setOption(StandardSocketOptions.TCP_NODELAY, true)
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, false)
    }


    suspend inline fun <T> read(size: Int, block: ByteBuffer.() -> T): T {

        val byteBuffer = DirectByteBufferPool.take(size)

        if (IS_DEBUGGING) {
            println("Reading: $size bytes, Remaining: ${byteBuffer.remaining()}")
        }

        suspendCoroutine<Unit> { continuation ->
            channel.read(byteBuffer, continuation, ReadCompletionHandler)
        }

        if (IS_DEBUGGING) {
            println("Read: $size bytes")
        }

        byteBuffer.flip()

        val value = block(byteBuffer)
        DirectByteBufferPool.give(byteBuffer)

        return value
    }

    suspend fun readByte(): Byte {
        return read(Byte.SIZE_BYTES) { get() }
    }

    suspend fun readShort(): Short {
        return read(Short.SIZE_BYTES) { short }
    }

    suspend fun readInt(): Int {
        return read(Int.SIZE_BYTES) { int }
    }

    suspend fun readLong(): Long {
        return read(Long.SIZE_BYTES) { long }
    }

    suspend fun readFloat(): Float {
        return read(Float.SIZE_BYTES) { float }
    }

    suspend fun readDouble(): Double {
        return read(Double.SIZE_BYTES) { double }
    }


    suspend fun readString(): String {

        val size = readShort().toInt()
        val data = ByteArray(size)

        read(size) {
            get(data)
        }

        return data.decodeToString()
    }

    fun queue(vararg packets: Packet) {
        packetQueue.addAll(packets)
    }

    suspend fun queueAndFlush(vararg packets: Packet) {
        queue(*packets)
        flush()
    }


    suspend fun flush() {

        var bytes = 0

        packetQueue.clearingForEach { packet ->
            packet.compile {

                it.flip()
                bytes += it.remaining()

                suspendCoroutine<Unit> { continuation ->
                    channel.write(it, continuation, WriteCompletionHandler)
                }
            }

        }

        if (IS_DEBUGGING) {
            println("Wrote: $bytes bytes")
        }

    }


    fun close() {
        channel.close()
        Netlius.running--
    }


    private inline fun <T> MutableCollection<T>.clearingForEach(block: (T) -> Unit) {

        val iterator = iterator()

        while (iterator.hasNext()) {
            val next = iterator.next()
            iterator.remove()
            block(next)
        }
    }


    object ReadCompletionHandler : CompletionHandler<Int, Continuation<Unit>> {

        override fun completed(result: Int, attachment: Continuation<Unit>) {

            attachment.resume(Unit)

            if (IS_DEBUGGING) {
                println("Resumed reading")
            }
        }

        override fun failed(exc: Throwable, attachment: Continuation<Unit>) {
            attachment.resumeWithException(exc)
        }

    }

    object WriteCompletionHandler : CompletionHandler<Int, Continuation<Unit>> {

        override fun completed(result: Int, attachment: Continuation<Unit>) {
            attachment.resume(Unit)
        }

        override fun failed(exc: Throwable, attachment: Continuation<Unit>) {
            attachment.resumeWithException(exc)
        }

    }


    companion object {

        const val IS_DEBUGGING = false

    }

}