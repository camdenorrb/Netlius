package me.camdenorrb.netlius

import kotlinx.coroutines.*
import me.camdenorrb.netlius.net.Packet
import java.nio.MappedByteBuffer

// This project should essentially be a bridge between the driver and JVM along with some coroutines
// TODO: Could potentially use https://github.com/rambodrahmani/linux-pspat for now
object Main {

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {

        val server = Netlius.server("127.0.0.1", 25565)

        server.onConnect {
            while (true) {
                println(readString())
            }
        }

        var count = 0

        repeat(100) {
            val client = Netlius.clientSuspended("127.0.0.1", 25565)
            client.queueAndFlush(Packet().string("Meow${count++}"))
        }


        //println(server.clients.size)
        //println(server.clients.firstOrNull()?.channel?.isOpen)

    }

}
