package me.camdenorrb.netlius

import kotlinx.coroutines.runBlocking
import kotlin.test.Test

class ClientTest {

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