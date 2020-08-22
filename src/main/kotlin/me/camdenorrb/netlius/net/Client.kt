package me.camdenorrb.netlius.net

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import me.camdenorrb.netlius.Netlius
import java.net.StandardSocketOptions
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.ClosedChannelException
import java.nio.channels.CompletionHandler
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.coroutines.*
import kotlin.jvm.Throws

// https://www.baeldung.com/java-nio2-async-socket-channel
// TODO: Use a different coroutine for writing and reading

typealias ClientListener = (Client) -> Unit

class Client internal constructor(channel: AsynchronousSocketChannel, val byteBufferPool: DirectByteBufferPool) {

    val packetQueue = mutableListOf<Packet>()

    var channel = channel
        private set

    /*
    var isClosing = false
        private set
    */

    val listeners = EnumMap<Event, MutableList<ClientListener>>(Event::class.java)


    init {
        channel.setOption(StandardSocketOptions.SO_RCVBUF, BUFFER_SIZE)
        channel.setOption(StandardSocketOptions.SO_SNDBUF, BUFFER_SIZE)
        channel.setOption(StandardSocketOptions.TCP_NODELAY, true)
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, false)
    }

    fun onConnect(block: (Client) -> Unit) {
        listeners.getOrPut(Event.CONNECT, { mutableListOf() }) += block
    }

    fun onDisconnect(block: (Client) -> Unit) {
        listeners.getOrPut(Event.DISCONNECT, { mutableListOf() }) += block
    }


    //This seems to cause issues
    // TODO: Check for InterruptedByTimeoutException and disconnect if so
    suspend inline fun <T : Any> read(size: Int, block: ByteBuffer.() -> T): T {

        lateinit var value: T

        byteBufferPool.take(size) { byteBuffer ->

            if (IS_DEBUGGING) {
                println("Reading: $size bytes, Remaining: ${byteBuffer.remaining()}")
            }

            suspendCoroutine<Unit> { continuation ->
                try {
                    channel.read(byteBuffer, 30, TimeUnit.SECONDS, continuation, ReadCompletionHandler)
                }
                catch (ex: Exception) {
                    close()
                }
            }

            if (IS_DEBUGGING) {
                println("Read: $size bytes")
            }

            byteBuffer.flip()

            value = block(byteBuffer)
        }

        //println("Size: $size, Capacity: ${byteBuffer.capacity()}, IsReadOnly: ${byteBuffer.isReadOnly}, IsDirect: ${byteBuffer.isDirect}, Order: ${byteBuffer.order()}, Limit: ${byteBuffer.limit()}, Position: ${byteBuffer.position()} Remaining: ${byteBuffer.remaining()}, HasRemaining: ${byteBuffer.hasRemaining()}}")

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
            get(0, data)
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
        packetQueue.clearingForEach { packet ->
            packet.writeQueue.forEach { writeTask ->
                byteBufferPool.take(writeTask.size) { byteBuffer ->

                    writeTask(byteBuffer)
                    byteBuffer.flip()

                    suspendCoroutine<Unit> { continuation ->
                        try {
                            channel.write(byteBuffer, 30, TimeUnit.SECONDS, continuation, WriteCompletionHandler)
                        }
                        catch (ex: Exception) {
                            close()
                        }
                    }
                }
            }
        }
    }


    @Throws(AsynchronousCloseException::class, BufferUnderflowException::class)
    fun close() {

        listeners[Event.DISCONNECT]?.clearingForEach {
            it.invoke(this)
        }

        channel.close()
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

    // Turn into a sealed class if you need a variety of parameters
    enum class Event {
        CONNECT,
        DISCONNECT,
    }


    companion object {

        const val BUFFER_SIZE = 8_192

        const val IS_DEBUGGING = false

    }

}