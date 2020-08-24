package me.camdenorrb.netlius

import kotlinx.coroutines.runBlocking

// This project should essentially be a bridge between the driver and JVM along with some coroutines
// TODO: Could potentially use https://github.com/rambodrahmani/linux-pspat for now
object Main {

    @JvmStatic
    fun main(args: Array<String>): Unit = runBlocking {
        Netlius.server("127.0.0.1", 25565)
    }

}
