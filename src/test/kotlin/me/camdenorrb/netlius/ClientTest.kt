package me.camdenorrb.netlius

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import me.camdenorrb.netlius.net.Packet
import kotlin.test.Test

class ClientTest {

    @Test
    fun `attack of the clients part 2 season 4`() = runBlocking {

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


        assert(count.value == 101)

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