package me.camdenorrb.netlius

import kotlinx.coroutines.asCoroutineDispatcher
import me.camdenorrb.netlius.net.Client
import me.camdenorrb.netlius.net.Server
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object Netlius {

    // TODO: Use a ThreadPool coroutine context and it'll do this for you
    private val threadPool = Executors.newCachedThreadPool().asCoroutineDispatcher()

    fun client(ip: String, port: Int): Client {

        val channel = AsynchronousSocketChannel.open()
        channel.connect(InetSocketAddress(ip, port)).get(30, TimeUnit.SECONDS)

        return Client(channel)
    }

    suspend fun clientSuspending(ip: String, port: Int): Client {

        lateinit var channel: AsynchronousSocketChannel

        suspendCoroutine<Unit> { continuation ->
            channel = AsynchronousSocketChannel.open()
            channel.connect(InetSocketAddress(ip, port), continuation, ClientCompletionHandler)
        }

        return Client(channel)
    }

    fun server(ip: String, port: Int): Server {
        return Server(ip, port).apply { start() }
    }

    fun stop() {
        try {
            threadPool.close()
        }
        catch (ex: Exception) {
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