package me.camdenorrb.netlius.net

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import me.camdenorrb.kcommons.io.ByteBufferReaderChannel
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
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// https://www.baeldung.com/java-nio2-async-socket-channel
// TODO: Use a different coroutine for writing and reading

typealias ClientListener = (Client) -> Unit

// TODO: Implement compression
// TODO: Add timeout option for everything
class Client internal constructor(channel: AsynchronousSocketChannel, val byteBufferPool: DirectByteBufferPool) : ByteBufferReaderChannel {

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
    override suspend inline fun <T> suspendRead(size: Int, block: ByteBuffer.() -> T): T {

        byteBufferPool.take(size) { byteBuffer ->
            readTo(size, byteBuffer)
            return block(byteBuffer.flip())
        }

        error("Unable to take from ByteBufferPool?")
    }

    suspend fun readTo(size: Int, byteBuffer: ByteBuffer, timeoutMS: Long = 30_000) {

        if (IS_DEBUGGING) {
            println("Reading: $size bytes, Remaining: ${byteBuffer.remaining()}")
        }

        try {

            if (size > 0) {
                readLock.withLock {
                    suspendCoroutine<Unit> { continuation ->
                        channel.read(byteBuffer, timeoutMS, TimeUnit.MILLISECONDS, continuation, ReadCompletionHandler)
                    }
                }
            }

            if (IS_DEBUGGING) {
                println("Read: $size bytes")
            }

        }
        catch (ex: Exception) {
            close()
            throw ex
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
        packetQueue.clearingForEach { packet ->
            byteBufferPool.take(packet.size) { byteBuffer ->

                packet.writeToBuffer(byteBuffer)
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