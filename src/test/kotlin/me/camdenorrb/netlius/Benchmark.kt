package me.camdenorrb.netlius

/*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import me.camdenorrb.netlius.net.DirectByteBufferPool
import me.camdenorrb.netlius.net.Packet
import tech.poder.podercord.networking.MooDirectByteBufferPool
import java.nio.ByteBuffer
import kotlin.system.measureNanoTime
import kotlin.test.Test


class Benchmark {

    @Test
    fun `echo benchmark`() {
        val server = Netlius.server("127.0.0.1", 10012)
        val client = Netlius.client("127.0.0.1", 10012)

        val serverTimeMS = atomic(0L)
        val clientTimeMS = atomic(0L)

        val packet = Packet().byte(0)

        server.onConnect { client ->
            repeat(DEFAULT_CYCLES * 2) {
                serverTimeMS += measureNanoTime {
                    client.suspendReadByte()
                }
            }
        }

        repeat(2) {

            runBlocking {
                repeat(DEFAULT_CYCLES) {
                    clientTimeMS += measureNanoTime {
                        client.queueAndFlush(packet)
                    }
                }
            }

            val averageNS = (clientTimeMS.value + serverTimeMS.value) / DEFAULT_CYCLES
            println("$averageNS nanoseconds per call")

            clientTimeMS.getAndSet(0)
            serverTimeMS.getAndSet(0)
        }
    }

    @Test
    fun `multithreaded echo benchmark`() {
        val server = Netlius.server("127.0.0.1", 10013)
        val client = Netlius.client("127.0.0.1", 10013)

        val serverTimeMS = atomic(0L)
        val clientTimeMS = atomic(0L)

        val packet = Packet().byte(0)

        server.onConnect { client ->
            repeat(DEFAULT_CYCLES * 2) {
                serverTimeMS += measureNanoTime {
                    client.suspendReadByte()
                }
            }
        }

        repeat(2) {
            runBlocking {

                (0..DEFAULT_CYCLES).map {
                    async(Dispatchers.IO) {
                        clientTimeMS += measureNanoTime {
                            client.queueAndFlush(packet)
                        }
                    }
                }.awaitAll()


                val averageNS = (clientTimeMS.value + serverTimeMS.value) / DEFAULT_CYCLES
                println("$averageNS nanoseconds per call")

                clientTimeMS.getAndSet(0)
                serverTimeMS.getAndSet(0)
            }
        }
    }

    @Test
    fun katBufferConcurrentSpeedTest() {

        repeat(1) {

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
            check(pool.byteBuffers.size == 20) {
                "Didn't get back all bytebuffers"
            }
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

                //val pool = DirectByteBufferPool(20)

                val totalTimeNS = (1..DEFAULT_CYCLES).sumByDouble {
                    measureNanoTime {
                        //pool.take(1) {}
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


    /*
    @Test
    fun directByteBufferAllocateSpeedTest() {

        repeat(5) {

            val totalAllocateTime = atomic(0L)

            runBlocking {
                (1..100_000).map {
                    async(Netlius.threadPoolDispatcher, CoroutineStart.LAZY) {
                        totalAllocateTime += (measureNanoTime {
                            // Takes 7 milliseconds per iteration at size 1280000
                            ByteBuffer.allocateDirect(1280000)
                        })
                    }
                }.awaitAll()
            }

            println(totalAllocateTime.value / 100_000)
        }
    }*/



    companion object {

        const val DEFAULT_CYCLES = 1_000

    }

}
*/