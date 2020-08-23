package me.camdenorrb.netlius

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.runBlocking
import me.camdenorrb.netlius.net.Packet
import kotlin.math.max
import kotlin.system.measureNanoTime
import kotlin.test.Test

class Benchmark {

    @Test
    fun `echo benchmark`() {

        val server = Netlius.server("127.0.0.1", 12345)
        val client = Netlius.client("127.0.0.1", 12345)

        val serverTimeMS = atomic(0L)
        val clientTimeMS = atomic(0L)

        server.onConnect {
            repeat(DEFAULT_CYCLES) {
                serverTimeMS += measureNanoTime {
                    readByte()
                }
            }
        }

        runBlocking {

            val packet = Packet().byte(0)

            repeat(DEFAULT_CYCLES) {
                clientTimeMS += measureNanoTime {
                    client.queueAndFlush(packet)
                }
            }
        }

        val averageNS = max(clientTimeMS.value, serverTimeMS.value) / DEFAULT_CYCLES
        println("$averageNS nanoseconds per call")
    }

    companion object {

        const val DEFAULT_CYCLES = 1_000_000

    }

}