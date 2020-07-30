package me.camdenorrb.netlius.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import me.camdenorrb.netlius.Netlius
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.*
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// TODO: Make an object thread that prevents the stopping of the program until count of servers is 0
// TODO: Above can be moved to Netlius and Netlius can check this class's running count

// TODO: Figure out how to detect disconnect
class Server internal constructor(val ip: String, val port: Int) {

    val clients = mutableListOf<Client>()


    private var onStart: Server.() -> Unit = {}

    private var onStop: Server.() -> Unit = {}

    private var onConnect: Client.() -> Unit = {}

    private var onDisconnect: Client.() -> Unit = {}


    var isRunning = false
        private set

    lateinit var channel: AsynchronousServerSocketChannel
        private set


    fun start() {

        if (isRunning) {
            return
        }

        channel = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(ip, port))

        isRunning = true

        GlobalScope.launch((Netlius.cachedThreadPoolDispatcher)) {

            while (isRunning) {

                val client = suspendCoroutine<Client> { continuation ->
                    channel.accept(continuation, AcceptCompletionHandler)
                }

                onConnect(client)
                clients += client
            }
        }

        onStart()
        Netlius.running++
    }

    fun stop() {

        if (!isRunning) {
            return
        }

        onStop()

        clients.forEach(Client::close)
        clients.clear()

        Netlius.running--
        isRunning = false
    }



    fun onConnect(block: Client.() -> Unit) {
        onConnect = block
    }


    object AcceptCompletionHandler : CompletionHandler<AsynchronousSocketChannel, Continuation<Client>> {

        override fun completed(result: AsynchronousSocketChannel, attachment: Continuation<Client>) {
            attachment.resume(Client(result))
        }

        override fun failed(exc: Throwable, attachment: Continuation<Client>) {
            attachment.resumeWithException(exc)
        }

    }

}