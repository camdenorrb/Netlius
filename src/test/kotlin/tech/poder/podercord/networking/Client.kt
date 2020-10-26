package tech.poder.podercord.networking

import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.net.StandardSocketOptions
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.max

class Client(private val channel: AsynchronousSocketChannel = AsynchronousSocketChannel.open()) {


    companion object {
        internal val threadPoolDispatcher =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() + 2).asCoroutineDispatcher()
        internal val scope = CoroutineScope(threadPoolDispatcher)
    }

    init {
        channel.setOption(StandardSocketOptions.TCP_NODELAY, true)
        channel.setOption(StandardSocketOptions.SO_KEEPALIVE, false)
    }

    private val queue = ConcurrentLinkedQueue<ByteBuffer>()

    fun connect(socketAddress: SocketAddress) {
        channel.connect(socketAddress).get()
    }

    private val flushLock = Mutex()
    private val lastFlush = AtomicLong(System.currentTimeMillis())
    private val isQueued = AtomicBoolean(false)
    private val queueLock = Mutex()

    private val readLock = Mutex()


    suspend fun write(data: ByteBuffer, willFlush: Boolean) {
        queue.add(data)
        if (willFlush && !isQueued.acquire) {
            queueLock.withLock {
                if (!isQueued.acquire) {
                    isQueued.set(true)
                    scope.launch {
                        val remaining = max(1, (System.currentTimeMillis() - lastFlush.get()) - 100)
                        println(remaining)
                        //delay(remaining)
                        flushBlocking()
                    }
                }
            }
        }
    }


    private suspend fun flushBlocking() {
        flushLock.withLock {
            while (queue.isNotEmpty()) {
                val x = queue.remove()
                suspendCoroutine<Unit> { continuation ->
                    channel.write(x, continuation, Handler)
                }
            }
        }
    }

    fun flush() {
        GlobalScope.launch {
            flushBlocking()
        }
    }

    suspend fun ensure(len: Int, buf: ByteBuffer) {

        suspendCoroutine<Unit> {
            buf.position(0)
            channel.read(buf, it, Handler)
        }

        buf.flip()
    }
}

fun main() = runBlocking(Client.threadPoolDispatcher) {

    val hostName = "localhost"
    val socketAddr: SocketAddress = InetSocketAddress(hostName, 12345)
    val client = Client()

    client.connect(socketAddr)

    val sendBuf = ByteBuffer.allocateDirect(12)
    val readBuf = ByteBuffer.allocateDirect(12)

    sendBuf.putInt(8)
    sendBuf.putLong(System.currentTimeMillis())
    sendBuf.flip()

    client.write(sendBuf, true)
    client.flush()

    while (true) {

        var rtt = 0L
        val iter = 10_000

        for (i in 0 until iter) {
            println(i)
            client.ensure(12, readBuf)
            //readBuf.flip()
            readBuf.int
            val ping = System.currentTimeMillis() - readBuf.long
            sendBuf.position(4)
            sendBuf.putLong(System.currentTimeMillis())
            sendBuf.flip()
            client.write(sendBuf, true)
            rtt += ping
        }

        println("Time for $iter; average rtt: ${rtt / iter}ms")
    }
}


object Handler : CompletionHandler<Int, Continuation<Unit>> {

    override fun completed(result: Int, attachment: Continuation<Unit>) {
        attachment.resume(Unit)
    }

    override fun failed(exc: Throwable, attachment: Continuation<Unit>) {

        println(exc::class.simpleName)
        exc.printStackTrace()

        when (exc::class.simpleName) {
            "IOException", "IllegalStateException" -> attachment.resumeWithException(exc)
            "ClosedChannelException", "AsynchronousCloseException" -> attachment.resume(Unit)
            else -> {
                println(exc::class.simpleName)
                exc.printStackTrace()
                attachment.resume(Unit)
            }
        }
    }
}