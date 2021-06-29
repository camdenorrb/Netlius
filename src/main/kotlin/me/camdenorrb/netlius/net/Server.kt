package me.camdenorrb.netlius.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.camdenorrb.netlius.Netlius
import java.io.EOFException
import java.net.InetSocketAddress
import java.net.StandardSocketOptions
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

// TODO: Make an object thread that prevents the stopping of the program until count of servers is 0
// TODO: Above can be moved to Netlius and Netlius can check this class's running count

// TODO: Figure out how to detect disconnect
// TODO: Add readTimeouts
class Server internal constructor(val ip: String, val port: Int, val defaultTimeoutMS: Long = 30_000) {

    val clients = ConcurrentLinkedQueue<Client>()


    private var onStartListeners = mutableListOf<Server.() -> Unit>()

    private var onStopListeners = mutableListOf<Server.() -> Unit>()

    // TODO: Add a disconnect listener
    private var onConnectListeners = mutableListOf<suspend (Client) -> Unit>()


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
        channel.setOption(StandardSocketOptions.SO_RCVBUF, Netlius.DEFAULT_BUFFER_SIZE)

        isRunning = true

        CoroutineScope(Netlius.threadPoolDispatcher).launch {

            while (isRunning) {

                val client = suspendCoroutine<Client> { continuation ->
                    channel.accept(continuation, AcceptCompletionHandler(defaultTimeoutMS))
                }

                client.channel.setOption(StandardSocketOptions.SO_RCVBUF, Netlius.DEFAULT_BUFFER_SIZE)
                client.channel.setOption(StandardSocketOptions.SO_SNDBUF, Netlius.DEFAULT_BUFFER_SIZE)
                client.channel.setOption(StandardSocketOptions.TCP_NODELAY, true)
                client.channel.setOption(StandardSocketOptions.SO_KEEPALIVE, true)

                client.onDisconnect {
                    clients.remove(it)
                }

                launch(Netlius.threadPoolDispatcher) {
                    try {
                        onConnectListeners.forEach { it(client) }
                    } catch (ex: Exception) {
                        if (!isClosing && ex !is EOFException) {
                            throw ex
                        }
                    }
                }

                clients += client
            }
        }

        onStartListeners.forEach { it(this) }
    }

    fun stop() {

        if (!isRunning) {
            return
        }

        isClosing = true

        onStopListeners.forEach { it(this) }
        clients.clearingForEach(Client::close)

        channel.close()

        isRunning = false
    }



    fun onConnect(block: suspend (Client) -> Unit) {
        onConnectListeners.add(block)
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

        const val IS_DEBUGGING = false

    }

    class AcceptCompletionHandler(val defaultTimeoutMS: Long) : CompletionHandler<AsynchronousSocketChannel, Continuation<Client>> {

        override fun completed(result: AsynchronousSocketChannel, attachment: Continuation<Client>) {
            attachment.resume(Client(result, Netlius.byteBufferPool, defaultTimeoutMS))
        }

        override fun failed(exc: Throwable, attachment: Continuation<Client>) {
            attachment.resumeWithException(exc)
        }

    }

}