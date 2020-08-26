package me.camdenorrb.netlius

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import me.camdenorrb.netlius.net.Packet
import kotlin.system.exitProcess
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientTest {

    @Test
    fun `client single message test`() {

        val server = Netlius.server("127.0.0.1", 25565)

        server.onConnect { client ->
            while (client.channel.isOpen) {
                println(client.readString())
            }
        }

        val client = Netlius.client("127.0.0.1", 25565)
        val packet = Packet().string("Meow")

        runBlocking {
            client.queueAndFlush(packet)
            delay(10000)
        }
    }

    @Test
    fun `client to server local throughput speedtest`() {

        val server = Netlius.server("127.0.0.1", 25565)
        val bytesRead = atomic(0L)
        val bufferSizeAsLong = Netlius.DEFAULT_BUFFER_SIZE.toLong()

        server.onConnect {
            while (true) {
                it.read(Netlius.DEFAULT_BUFFER_SIZE) {
                    bytesRead += bufferSizeAsLong
                }
            }
        }

        val client = Netlius.client("127.0.0.1", 25565)

        repeat(100) {

            val packet = Packet().bytes(ByteArray(Netlius.DEFAULT_BUFFER_SIZE) { 1 })

            val job = GlobalScope.launch {
                while (true) {
                    client.queueAndFlush(packet)
                }
            }

            runBlocking {
                job.cancel()
                delay(1_000)
                println("${bytesRead.value / 1024 / 1024}MB/s")
                bytesRead.getAndSet(0)
            }
        }
    }

    @Test
    fun `client multipart message test`() {

        val server = Netlius.server("127.0.0.1", 25565)

        server.onConnect { client ->
            while (client.channel.isOpen) {
                assertEquals("${client.readString()}${client.readString()}".also { println(it) }, "12")
            }
        }

        val client = Netlius.client("127.0.0.1", 25565)
        val packet = Packet().string("1").string("2")

        runBlocking {
            client.queueAndFlush(packet)
            delay(10000)
        }
    }

    @Test
    fun `client prepend message test`() {

        val server = Netlius.server("127.0.0.1", 25565)
        var succeeded = false

        server.onConnect { client ->
            while (client.channel.isOpen) {
                assertEquals("${client.readString()}${client.readString()}${client.readString()}", "123")
                succeeded = true
            }
        }

        val client = Netlius.client("127.0.0.1", 25565)

        val packet = Packet().string("3").prepend {
            string("1")
            string("2")
        }

        runBlocking {
            client.queueAndFlush(packet)
            // TODO: Figure out a way to make this delay not needed
            delay(10000)
        }

        assert(succeeded)
    }


    @Test
    fun `attack of the client part 1 season 1`() = runBlocking {

        val server = Netlius.server("127.0.0.1", 25565)

        server.onConnect { client ->
            println(measureTimeMillis {
                repeat(7_000) {
                    client.readString()
                }
            })
        }

        repeat(100_000) {

            val client = Netlius.clientSuspending("127.0.0.1", 25565)
            val meowPacket = Packet().string("Meow")

            //client.queueAndFlush(meowPacket)

            (1..7000).map {
                async(Netlius.threadPoolDispatcher, CoroutineStart.LAZY) {
                    client.queueAndFlush(meowPacket)
                }
            }.awaitAll()
        }

        delay(10_000)
    }

    @Test
    fun `attack of the clients part 2 season 4`() = runBlocking {

        val server = Netlius.server("127.0.0.1", 25565)

        server.onConnect { client ->
            while (client.channel.isOpen) {
                client.readString()
                client.readString()
            }
        }

        val count = atomic(0)

        (1..1_000).map {
            async(Netlius.threadPoolDispatcher, CoroutineStart.LAZY) {
                val client = Netlius.clientSuspending("127.0.0.1", 25565)
                client.queueAndFlush(Packet().string("Meow").string("Test${count.getAndAdd(1)}"))
            }
        }.awaitAll()

        assertEquals(count.value, 1_000)

        server.stop()
        Netlius.stop()
    }

    // Takes 30 seconds to run due to timeout
    @Test
    fun `client auto close test`() {

        val server = Netlius.server("127.0.0.1", 12345)
        val client = Netlius.client("127.0.0.1", 12345)

        client.onDisconnect {
            println("Disconnected :D")
        }

        server.stop()

        runBlocking {
            try {
                // This should auto close it as the server shouldn't be on
                client.readByte()
            }
            catch (ex: Exception) {
                // Ignore
            }
        }

        assert(!client.channel.isOpen)
    }

}