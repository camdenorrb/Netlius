package me.camdenorrb.netlius

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.camdenorrb.netlius.net.Packet
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientTest {

    @Test
    fun `client single message test`() {

        val server = Netlius.server("127.0.0.1", 25565)

        server.onConnect {
            while (channel.isOpen) {
                println(readString())
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
    fun `client multipart message test`() {

        val server = Netlius.server("127.0.0.1", 25565)

        server.onConnect {
            while (channel.isOpen) {
                assertEquals("${readString()}${readString()}", "12")
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

        server.onConnect {
            while (channel.isOpen) {
                println("Thing: ${readString()}")
            }
        }

        val client = Netlius.client("127.0.0.1", 25565)

        val packet = Packet().string("3").prepend {
            string("1")
            string("2")
        }

        runBlocking {
            client.queueAndFlush(packet)
            delay(10000)
        }
    }


    @Test
    fun `attack of the clients part 2 season 4`() = runBlocking {

        val server = Netlius.server("127.0.0.1", 25565)

        server.onConnect {
            while (channel.isOpen) {
                println("${readString()}:${readString()}")
            }
        }

        val count = atomic(0)

        (0..100).map {
            async(Netlius.threadPoolDispatcher) {
                val client = Netlius.clientSuspending("127.0.0.1", 25565)
                client.queueAndFlush(Packet().string("Meow").string("Test${count.getAndAdd(1)}"))
            }
        }.awaitAll()

        delay(2000)

        assertEquals(count.value, 101)

        server.stop()
        Netlius.stop()
    }

    // Takes 30 seconds to run due to timeout
    @Test
    fun `client auto close test`() {

        val server = Netlius.server("127.0.0.1", 12345)
        val client = Netlius.client("127.0.0.1", 12345)

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