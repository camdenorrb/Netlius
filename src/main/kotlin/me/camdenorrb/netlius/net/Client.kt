package me.camdenorrb.netlius.net

import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import me.camdenorrb.netlius.Netlius
import java.io.EOFException
import java.net.StandardSocketOptions
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.*

// https://www.baeldung.com/java-nio2-async-socket-channel
// TODO: Use a different coroutine for writing and reading

typealias ClientListener = (Client) -> Unit

// TODO: Implement compression
class Client internal constructor(channel: AsynchronousSocketChannel) {

    val packetQueue = ConcurrentLinkedQueue<Packet>()

    val listeners = EnumMap<Event, MutableList<ClientListener>>(Event::class.java)


    val readLock = Mutex()

    val writeLock = Mutex()


    val readBuffer = ByteBuffer.allocateDirect(Netlius.DEFAULT_BUFFER_SIZE)

    val writeBuffer = ByteBuffer.allocateDirect(Netlius.DEFAULT_BUFFER_SIZE)


    var channel = channel
        private set


    init {
        channel.setOption(StandardSocketOptions.SO_RCVBUF, Netlius.DEFAULT_BUFFER_SIZE)
        channel.setOption(StandardSocketOptions.SO_SNDBUF, Netlius.DEFAULT_BUFFER_SIZE)
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

        readLock.lock(readBuffer)

        try {

            readBuffer.clear().limit(size)

            if (IS_DEBUGGING) {
                println("Reading: $size bytes, Remaining: ${readBuffer.remaining()}")
            }

            suspendCoroutine<Unit> { continuation ->
                channel.read(readBuffer, 30, TimeUnit.SECONDS, continuation, ReadCompletionHandler)
            }

            if (IS_DEBUGGING) {
                println("Read: $size bytes")
            }

            readLock.unlock()
            val value = block(readBuffer.flip())

            return value

        } catch (ex: Exception) {
            close()
            throw ex
        }
    }

    suspend fun readByte(): Byte {
        return read(Byte.SIZE_BYTES) { get() }
    }

    suspend fun readBytes(n: Int): ByteArray {
        return read(n * Byte.SIZE_BYTES) {
            ByteArray(n) {
                get(it)
            }
        }
    }

    suspend fun readBoolean(): Boolean {
        return when (val read = readByte().toInt()) {

            0 -> false
            1 -> true

            else -> error("Unable to read boolean '$read'")
        }
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
        return readBytes(size).decodeToString()
    }

    fun queue(vararg packets: Packet) {
        packetQueue.addAll(packets)
    }

    suspend fun queueAndFlush(vararg packets: Packet) {
        queue(*packets)
        flush()
    }

    suspend fun flush() {

        writeLock.lock()

        packetQueue.clearingForEach { packet ->

            packet.writeQueue.forEach { writeTask ->
                writeTask(writeBuffer)
            }

            writeBuffer.flip()

            try {

                suspendCoroutine<Unit> { continuation ->
                    channel.write(writeBuffer, 30, TimeUnit.SECONDS, continuation, WriteCompletionHandler)
                }

                writeBuffer.clear()
            }
            catch (ex: Exception) {
                close()
                throw ex
            }
        }

        writeLock.unlock()
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

            if (result == -1) {
                attachment.resumeWithException(EOFException())
                return
            }

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

        const val IS_DEBUGGING = false

    }

}