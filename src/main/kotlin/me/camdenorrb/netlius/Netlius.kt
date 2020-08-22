package me.camdenorrb.netlius

import me.camdenorrb.netlius.net.Client
import me.camdenorrb.netlius.net.Server
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object Netlius {

    @Volatile
    var isRunning = false
        private set

    val keepRunningThread = thread(false) {
        while (isRunning) {
            Thread.onSpinWait()
        }
    }

    fun client(ip: String, port: Int): Client {

        if (!keepRunningThread.isAlive) {
            isRunning = true
            keepRunningThread.start()
        }

        val channel = AsynchronousSocketChannel.open()
        channel.connect(InetSocketAddress(ip, port)).get(30, TimeUnit.SECONDS)

        return Client(channel)
    }

    suspend fun clientSuspended(ip: String, port: Int): Client {

        if (!keepRunningThread.isAlive) {
            isRunning = true
            keepRunningThread.start()
        }

        lateinit var channel: AsynchronousSocketChannel

        suspendCoroutine<Unit> { continuation ->
            channel = AsynchronousSocketChannel.open()
            channel.connect(InetSocketAddress(ip, port), continuation, ClientCompletionHandler)
        }

        return Client(channel)
    }

    fun server(ip: String, port: Int): Server {

        if (!keepRunningThread.isAlive) {
            isRunning = true
            keepRunningThread.start()
        }

        return Server(ip, port).apply { start() }
    }

    fun stop() {
        if (keepRunningThread.isAlive) {
            isRunning = false
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