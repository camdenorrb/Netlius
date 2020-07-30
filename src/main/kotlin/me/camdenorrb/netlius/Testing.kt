package me.camdenorrb.netlius

/*
import kotlinx.atomicfu.AtomicInt
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import me.camdenorrb.netlius.net.DirectByteBufferPool
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object Testing {

    val channel = Channel<String>(20)

    val threads = Executors.newCachedThreadPool()

    @JvmStatic
    fun main(args: Array<String>) = runBlocking {

        val coroutineScope = CoroutineScope(Dispatchers.Default)

        var count = AtomicInteger(0)
        val jobs = mutableListOf<Job>()

        repeat(100) {
            jobs += coroutineScope.launch {
                DirectByteBufferPool.give(DirectByteBufferPool.take(10))
                count.incrementAndGet()
            }
        }

        jobs.forEach {
            it.join()
        }

        println(count)

    }

    suspend fun callbackToSuspend() {
        suspendCoroutine<Unit> { continuation ->
            doCallbackJob {
                continuation.resume(Unit)
                println("Here")
            }
        }
    }

    fun doCallbackJob(block: () -> Unit) {
        threads.execute {
            Thread.sleep(1000)
            block()
        }
    }

}
*/
