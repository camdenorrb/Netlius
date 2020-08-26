package me.camdenorrb.netlius.net

import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.camdenorrb.netlius.Netlius
import me.camdenorrb.netlius.ext.decodeToString
import java.io.EOFException
import java.net.StandardSocketOptions
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.*

// https://www.baeldung.com/java-nio2-async-socket-channel
// TODO: Use a different coroutine for writing and reading

typealias ClientListener = (Client) -> Unit

// TODO: Implement compression
class Client internal constructor(channel: AsynchronousSocketChannel, val byteBufferPool: DirectByteBufferPool) {

    val packetQueue = ConcurrentLinkedQueue<Packet>()

    val listeners = EnumMap<Event, MutableList<ClientListener>>(Event::class.java)


    val readLock = Mutex()

    val writeLock = Mutex()


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

        byteBufferPool.take(size) { byteBuffer ->

            if (IS_DEBUGGING) {
                println("Reading: $size bytes, Remaining: ${byteBuffer.remaining()}")
            }

            try {

                readLock.withLock {
                    suspendCoroutine<Unit> { continuation ->
                        channel.read(byteBuffer, 30, TimeUnit.SECONDS, continuation, ReadCompletionHandler)
                    }
                }

                if (IS_DEBUGGING) {
                    println("Read: $size bytes")
                }

                return block(byteBuffer.flip())
            }
            catch (ex: Exception) {
                close()
                throw ex
            }
        }

        error("Unable to take from ByteBufferPool?")
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

    suspend fun readString(encoding: Charset = StandardCharsets.UTF_8): String {

        val size = readShort().toInt()

        return read(size) {
            this.decodeToString(encoding)
        }
    }

    fun queue(vararg packets: Packet) {
        packetQueue.addAll(packets)
    }

    suspend fun queueAndFlush(vararg packets: Packet) {
        queue(*packets)
        flush()
    }

    suspend fun flush() {
        byteBufferPool.take(Netlius.DEFAULT_BUFFER_SIZE) { byteBuffer ->
            packetQueue.clearingForEach { packet ->

                packet.writeQueue.forEach { writeTask ->
                    writeTask(byteBuffer)
                }

                byteBuffer.flip()

                try {
                    writeLock.withLock {
                        suspendCoroutine<Unit> { continuation ->
                            channel.write(byteBuffer, 30, TimeUnit.SECONDS, continuation, WriteCompletionHandler)
                        }
                    }
                }
                catch (ex: Exception) {
                    close()
                    throw ex
                }
                finally {
                    byteBuffer.clear()
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