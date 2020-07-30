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

    val cachedThreadPoolDispatcher = Executors.newCachedThreadPool().asCoroutineDispatcher()

    val keepRunningThread = thread(false) {
        while (running > 0) {
            Thread.sleep(Long.MAX_VALUE)
        }
    }

    // Updated by starting/stopping Clients and Servers
    internal var running = 0
        set(value) {

            if (value >= 1 && !keepRunningThread.isAlive) {
                keepRunningThread.start()
            }

            field = value
        }


    fun client(ip: String, port: Int): Client {

        val channel = AsynchronousSocketChannel.open()
        channel.connect(InetSocketAddress(ip, port)).get(1, TimeUnit.MINUTES)

        return Client(channel)
    }

    suspend fun clientSuspended(ip: String, port: Int): Client {

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


    object ClientCompletionHandler : CompletionHandler<Void, Continuation<Unit>> {

        override fun completed(result: Void?, attachment: Continuation<Unit>) {
            attachment.resume(Unit)
        }

        override fun failed(exc: Throwable, attachment: Continuation<Unit>) {
            attachment.resumeWithException(exc)
        }

    }

}