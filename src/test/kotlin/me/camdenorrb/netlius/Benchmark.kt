package me.camdenorrb.netlius

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import me.camdenorrb.netlius.net.DirectByteBufferPool
import me.camdenorrb.netlius.net.Packet
import tech.poder.podercord.networking.MooDirectByteBufferPool
import java.nio.ByteBuffer
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

        server.onConnect { client ->
            repeat(DEFAULT_CYCLES) {
                serverTimeMS += measureNanoTime {
                    client.readByte()
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

    @Test
    fun katBufferConcurrentSpeedTest() {

        repeat(10) {

            val pool = DirectByteBufferPool(20)
            val poolReallocateTime = atomic(0L)

            runBlocking {
                (1..DEFAULT_CYCLES).map {
                    async(Netlius.threadPoolDispatcher, CoroutineStart.LAZY) {
                        poolReallocateTime += (measureNanoTime {
                            pool.take(DEFAULT_BUFFER_SIZE) {}
                        })
                    }
                }.awaitAll()
            }

            println(poolReallocateTime.value / DEFAULT_CYCLES)
        }
    }

    @Test
    fun mooBufferConcurrentSpeedTest() {

        val pool = MooDirectByteBufferPool(20)
        val poolReallocateTime = atomic(0L)

        runBlocking {
            (1..DEFAULT_CYCLES).map {
                async(Netlius.threadPoolDispatcher, CoroutineStart.LAZY) {
                    poolReallocateTime += (measureNanoTime {
                        pool.take(1) {}
                    })
                }
            }.awaitAll()
        }

        println(poolReallocateTime.value / DEFAULT_CYCLES)
    }

    @Test
    fun katBufferSingleThreadSpeedTest() {

        var totalTotalTimeNS = 0L

        repeat(10) {
            runBlocking {

                val pool = DirectByteBufferPool(20)

                val totalTimeNS = (1..DEFAULT_CYCLES).sumByDouble {
                    measureNanoTime {
                        pool.take(1) {}
                    }.toDouble()
                }


                totalTotalTimeNS += totalTimeNS.toLong()
            }
        }

        println(totalTotalTimeNS / (DEFAULT_CYCLES * 10))
    }

    @Test
    fun mooBufferSingleThreadSpeedTest() {

        var totalTotalTimeNS = 0L

        repeat(10) {
            runBlocking {

                val pool = MooDirectByteBufferPool(20)

                val totalTimeNS = (1..DEFAULT_CYCLES).sumByDouble {
                    measureNanoTime {
                        pool.take(1) {}
                    }.toDouble()
                }

                totalTotalTimeNS += totalTimeNS.toLong()
            }
        }

        println(totalTotalTimeNS / (DEFAULT_CYCLES * 10))
    }


    @Test
    fun directByteBufferAllocateSpeedTest() {

        repeat(5) {

            val totalAllocateTime = atomic(0L)

            runBlocking {
                (1..DEFAULT_CYCLES).map {
                    async(Netlius.threadPoolDispatcher, CoroutineStart.LAZY) {
                        totalAllocateTime += (measureNanoTime {
                            ByteBuffer.allocateDirect(8192)
                        })
                    }
                }.awaitAll()
            }

            println(totalAllocateTime.value / DEFAULT_CYCLES)
        }
    }



    companion object {

        const val DEFAULT_CYCLES = 1_000_000

    }

}