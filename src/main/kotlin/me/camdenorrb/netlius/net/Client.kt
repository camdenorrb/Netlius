package me.camdenorrb.netlius.net

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
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// https://www.baeldung.com/java-nio2-async-socket-channel
// TODO: Use a different coroutine for writing and reading

typealias ClientListener = (Client) -> Unit

// TODO: Implement compression
// TODO: Add timeout option for everything
class Client internal constructor(
    channel: AsynchronousSocketChannel,
    val byteBufferPool: DirectByteBufferPool,
    val defaultTimeoutMS: Long = 30_000
) {

    val packetQueue = ConcurrentLinkedQueue<Packet>()

    val listeners = EnumMap<Event, ConcurrentLinkedQueue<ClientListener>>(Event::class.java)


    val readLock = Mutex()

    val writeLock = Mutex()


    var channel = channel
        private set


    init {
        channel.setOption(StandardSocketOptions.SO_RCVBUF, Netlius.DEFAULT_BUFFER_SIZE)
        channel.setOption(StandardSocketOptions.SO_SNDBUF, Netlius.DEFAULT_BUFFER_SIZE)
        channel.setOption(StandardSocketOptions.TCP_NODELAY, true)
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true)
    }

    fun onConnect(block: (Client) -> Unit) {
        listeners.getOrPut(Event.CONNECT) { ConcurrentLinkedQueue() } += block
    }

    fun onDisconnect(block: (Client) -> Unit) {
        listeners.getOrPut(Event.DISCONNECT) { ConcurrentLinkedQueue() } += block
    }


    //This seems to cause issues
    // TODO: Check for InterruptedByTimeoutException and disconnect if so
    suspend fun <T> suspendRead(size: Int, block: suspend ByteBuffer.() -> T): T {

        byteBufferPool.take(size) { byteBuffer ->
            readTo(size, byteBuffer, defaultTimeoutMS)
            return block(byteBuffer.flip())
        }

        error("Unable to take from ByteBufferPool?")
    }


    suspend fun suspendReadByte(): Byte {
        return suspendRead(Byte.SIZE_BYTES) { get() }
    }

    suspend fun suspendReadBytes(amount: Int): ByteArray {
        return ByteArray(amount) { suspendReadByte() }
    }

    suspend fun suspendReadBoolean(): Boolean {
        return when (val read = suspendReadByte().toInt()) {

            0 -> false
            1 -> true

            else -> error("Unable to read boolean '$read'")
        }
    }

    suspend fun suspendReadShort(): Short {
        return suspendRead(Short.SIZE_BYTES) { short }
    }

    suspend fun suspendReadInt(): Int {
        return suspendRead(Int.SIZE_BYTES) { int }
    }

    suspend fun suspendReadLong(): Long {
        return suspendRead(Long.SIZE_BYTES) { long }
    }

    suspend fun suspendReadFloat(): Float {
        return suspendRead(Float.SIZE_BYTES) { float }
    }

    suspend fun suspendReadDouble(): Double {
        return suspendRead(Double.SIZE_BYTES) { double }
    }

    suspend fun suspendReadString(encoding: Charset = Charsets.UTF_8): String {

        val size = suspendReadShort().toInt()

        return suspendRead(size) {
            this.decodeToString(encoding)
        }
    }

    suspend fun readTo(size: Int, byteBuffer: ByteBuffer, timeoutMS: Long = defaultTimeoutMS) {

        readLock.withLock {

            if (IS_DEBUGGING) {
                println("Reading: $size bytes, Remaining: ${byteBuffer.remaining()}")
            }

            try {

                var read = 0

                while (read < size) {
                    read += suspendCoroutine<Int> { continuation ->
                        channel.read(byteBuffer, timeoutMS, TimeUnit.MILLISECONDS, continuation, ReadCompletionHandler)
                    }
                }

                if (IS_DEBUGGING) {
                    println("Read: $size bytes")
                }

            } catch (ex: Exception) {
                close()
                throw ex
            }

        }
    }


    fun queue(vararg packets: Packet) {
        packetQueue.addAll(packets)
    }

    suspend fun queueAndFlush(vararg packets: Packet, timeoutMS: Long = defaultTimeoutMS) {
        queue(*packets)
        flush(timeoutMS)
    }

    suspend fun flush(timeoutMS: Long = defaultTimeoutMS) {
        writeLock.withLock {
            packetQueue.clearingForEach { packet ->
                byteBufferPool.take(packet.size) { byteBuffer ->

                    packet.writeToBuffer(byteBuffer)
                    byteBuffer.flip()

                    try {
                        while (byteBuffer.hasRemaining()) {
                            suspendCoroutine<Int> { continuation ->
                                channel.write(byteBuffer, timeoutMS, TimeUnit.MILLISECONDS, continuation, WriteCompletionHandler)
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
            block(iterator.next())
            iterator.remove()
        }
    }


    object ReadCompletionHandler : CompletionHandler<Int, Continuation<Int>> {

        override fun completed(result: Int, attachment: Continuation<Int>) {

            if (result == -1) {
                attachment.resumeWithException(EOFException())
                return
            }

            attachment.resume(result)

            if (IS_DEBUGGING) {
                println("Resumed reading")
            }
        }

        override fun failed(exc: Throwable, attachment: Continuation<Int>) {
            attachment.resumeWithException(exc)
        }

    }

    object WriteCompletionHandler : CompletionHandler<Int, Continuation<Int>> {

        override fun completed(result: Int, attachment: Continuation<Int>) {
            attachment.resume(result)
        }

        override fun failed(exc: Throwable, attachment: Continuation<Int>) {
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