package me.camdenorrb.netlius.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.camdenorrb.netlius.Netlius
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// TODO: Make an object thread that prevents the stopping of the program until count of servers is 0
// TODO: Above can be moved to Netlius and Netlius can check this class's running count

// TODO: Figure out how to detect disconnect
// TODO: Add readTimeouts
class Server internal constructor(val ip: String, val port: Int) {

    val clients = mutableListOf<Client>()


    private var onStart: Server.() -> Unit = {}

    private var onStop: Server.() -> Unit = {}

    private var onConnect: suspend Client.() -> Unit = {}

    private var onDisconnect: Client.() -> Unit = {}


    var isClosing = false
        private set

    var isRunning = false
        private set

    lateinit var channel: AsynchronousServerSocketChannel
        private set


    fun start() {

        if (isRunning) {
            return
        }

        channel = AsynchronousServerSocketChannel.open().bind(InetSocketAddress(ip, port))
        channel.setOption(StandardSocketOptions.SO_RCVBUF, BUFFER_SIZE)

        isRunning = true

        CoroutineScope(Dispatchers.Default).launch {

            while (isRunning) {

                val client = suspendCoroutine<Client> { continuation ->
                    channel.accept(continuation, AcceptCompletionHandler)
                }

                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        onConnect(client)
                    } catch (ex: Exception) {
                        if (!isClosing) {
                            throw ex
                        }
                    }
                }

                clients += client
            }
        }

        onStart()
    }

    fun stop() {

        if (!isRunning) {
            return
        }

        isClosing = true

        onStop()
        clients.clearingForEach(Client::close)

        isRunning = false
    }



    fun onConnect(block: suspend Client.() -> Unit) {
        onConnect = block
    }


    private inline fun <T> MutableCollection<T>.clearingForEach(block: (T) -> Unit) {

        val iterator = iterator()

        while (iterator.hasNext()) {
            val next = iterator.next()
            iterator.remove()
            block(next)
        }
    }


    companion object {

        const val BUFFER_SIZE = 8_192

        const val IS_DEBUGGING = false

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