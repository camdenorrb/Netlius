package me.camdenorrb.netlius

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.runBlocking
import me.camdenorrb.netlius.net.Packet
import kotlin.test.Test

class Benchmark {

    @Test
    fun `echo benchmark`() {

        val server = Netlius.server("127.0.0.1", 12345)
        val client = Netlius.client("127.0.0.1", 12345)

        val atomicTotalNS = atomic(0L)

        server.onConnect {
            repeat(DEFAULT_CYCLES) {
                atomicTotalNS += System.nanoTime() - readLong()
            }
        }

        runBlocking {
            repeat(DEFAULT_CYCLES) {
                client.queueAndFlush(Packet().long(System.nanoTime()))
            }
        }

        val averageNS = atomicTotalNS.value / DEFAULT_CYCLES
        println("$averageNS nanoseconds per call")
    }

    companion object {

        const val DEFAULT_CYCLES = 10_000_000

    }

}