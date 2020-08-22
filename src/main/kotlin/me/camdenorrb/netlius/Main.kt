package me.camdenorrb.netlius

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import me.camdenorrb.netlius.net.Packet

// This project should essentially be a bridge between the driver and JVM along with some coroutines
// TODO: Could potentially use https://github.com/rambodrahmani/linux-pspat for now
object Main {

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {

        val server = Netlius.server("127.0.0.1", 25565)

        server.onConnect {
            while (channel.isOpen) {
                println(readString())
            }
        }

        val count = atomic(0)

        (0..100).map {
            async(Netlius.threadPoolDispatcher) {
                val client = Netlius.clientSuspending("127.0.0.1", 25565)
                client.queueAndFlush(Packet().string("Test${count.getAndAdd(1)}"))
            }
        }.awaitAll()


        //println(DirectByteBufferPool.byteBuffers.firstEntry().value.size)

        delay(1000)


        println(count)

        server.stop()
        Netlius.stop()

        //println(server.clients.size)
        //println(server.clients.firstOrNull()?.channel?.isOpen)

    }

}
