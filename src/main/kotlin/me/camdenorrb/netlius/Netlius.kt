package me.camdenorrb.netlius

import kotlinx.coroutines.asCoroutineDispatcher
import me.camdenorrb.netlius.net.Client
import me.camdenorrb.netlius.net.DirectByteBufferPool
import me.camdenorrb.netlius.net.Server
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

// TODO: Make a class which takes in the bytebufferpool size, bytebufferpool, threadpool, etc
object Netlius {

    const val DEFAULT_BUFFER_SIZE = 65_536

    val byteBufferPool = DirectByteBufferPool(10)

    internal val threadPoolDispatcher = Executors.newFixedThreadPool(max(Runtime.getRuntime().availableProcessors(), 64)).asCoroutineDispatcher()


    fun client(ip: String, port: Int): Client {

        val channel = AsynchronousSocketChannel.open()
        channel.connect(InetSocketAddress(ip, port)).get(30, TimeUnit.SECONDS)

        return Client(channel, byteBufferPool)
    }

    suspend fun clientSuspending(ip: String, port: Int): Client {

        lateinit var channel: AsynchronousSocketChannel

        try {
            suspendCoroutine<Unit> { continuation ->
                channel = AsynchronousSocketChannel.open()
                channel.connect(InetSocketAddress(ip, port), continuation, ClientCompletionHandler)
            }
        }
        catch (ex: Exception) {
            throw ex // Rethrow because coroutines..... :C
        }

        return Client(channel, byteBufferPool)
    }

    fun server(ip: String, port: Int, autoStart: Boolean = true): Server {
        return Server(ip, port).apply { if (autoStart) start() }
    }

    fun stop() {
        threadPoolDispatcher.close()
    }


    object ClientCompletionHandler : CompletionHandler<Void, Continuation<Unit>> {

        override fun completed(result: Void?, attachment: Continuation<Unit>) {
            attachment.resume(Unit)
        }

        override fun failed(exc: Throwable, attachment: Continuation<Unit>) {
            attachment.resumeWithException(exc)
        }

    }


}