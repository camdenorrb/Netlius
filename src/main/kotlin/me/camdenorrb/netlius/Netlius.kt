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

object Netlius {

    internal val clientByteBufferPool = DirectByteBufferPool(10)

    internal val serverByteBufferPool = DirectByteBufferPool(10)

    internal val threadPoolDispatcher = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors()).asCoroutineDispatcher()


    fun client(ip: String, port: Int): Client {

        val channel = AsynchronousSocketChannel.open()
        channel.connect(InetSocketAddress(ip, port)).get(30, TimeUnit.SECONDS)

        return Client(channel, clientByteBufferPool)
    }

    suspend fun clientSuspending(ip: String, port: Int): Client {

        lateinit var channel: AsynchronousSocketChannel

        suspendCoroutine<Unit> { continuation ->
            channel = AsynchronousSocketChannel.open()
            channel.connect(InetSocketAddress(ip, port), continuation, ClientCompletionHandler)
        }

        return Client(channel, clientByteBufferPool)
    }

    fun server(ip: String, port: Int): Server {
        return Server(ip, port).apply { start() }
    }

    fun stop() {
        try {
            threadPoolDispatcher.close()
        }
        catch (ex: Exception) {
            ex.printStackTrace()
            // Ignored
        }
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