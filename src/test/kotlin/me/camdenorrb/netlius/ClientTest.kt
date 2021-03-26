package me.camdenorrb.netlius

/*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.*
import me.camdenorrb.netlius.ext.toByteArray
import me.camdenorrb.netlius.net.Packet
import kotlin.coroutines.suspendCoroutine
import kotlin.random.Random
import kotlin.system.measureTimeMillis
import kotlin.test.Test
import kotlin.test.assertEquals


class ClientTest {

    @Test
    fun ping() {

        val server = Netlius.server("127.0.0.1", 12345)

        server.onConnect { client ->
            while (client.channel.isOpen) {
                val sendTime = System.currentTimeMillis()
                client.queueAndFlush(Packet().byte(0))
                client.suspendReadByte()
                println(System.currentTimeMillis() - sendTime)
            }
        }

        val client = Netlius.client("127.0.0.1", 12345)

        runBlocking {
            repeat(1_000) {
                client.queueAndFlush(Packet().byte(0))
            }
            delay(10000)
        }

        server.stop()
    }

    @Test
    fun `the 5 big packets`() {

        val originalData = Random.nextBytes(100_000)
        val packet = Packet().bytes(originalData)

        val server = Netlius.server("127.0.0.1", 12345)

        server.onConnect { client ->
            while (client.channel.isOpen) {
                client.suspendRead(100_000) {
                    println(originalData.contentEquals(this.toByteArray()))
                }
            }
        }

        runBlocking {

            (0..5).map {
                async(start = CoroutineStart.LAZY) {
                    val client = Netlius.client("127.0.0.1", 12345)
                    println("Sending")
                    client.queueAndFlush(packet)
                    println("Sent")
                }
            }.awaitAll()

            delay(10_000_000)
        }

        server.stop()
    }

    @Test
    fun `client single message test`() {

        val server = Netlius.server("127.0.0.1", 12345)

        server.onConnect { client ->
            while (client.channel.isOpen) {
                println(client.suspendReadString())
            }
        }

        val client = Netlius.client("127.0.0.1", 12345)
        val packet = Packet().string("Meow")

        runBlocking {
            client.queueAndFlush(packet)
            delay(10000)
        }

        server.stop()
    }

    @Test
    fun `client to server local throughput speedtest`() {

        val server = Netlius.server("127.0.0.1", 12345)
        val bytesRead = atomic(0L)
        val bufferSizeAsLong = Netlius.DEFAULT_BUFFER_SIZE.toLong()

        server.onConnect {
            while (true) {
                it.suspendRead(Netlius.DEFAULT_BUFFER_SIZE) {
                    bytesRead += bufferSizeAsLong
                }
            }
        }

        repeat(100) {

            val packet = Packet().bytes(ByteArray(Netlius.DEFAULT_BUFFER_SIZE) { 1 })
            val client = Netlius.client("127.0.0.1", 12345)

            val job = GlobalScope.launch(Netlius.threadPoolDispatcher) {
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

        server.stop()
    }

    @Test
    fun `client multipart message test`() {

        val server = Netlius.server("127.0.0.1", 12345)

        server.onConnect { client ->
            while (client.channel.isOpen) {
                assertEquals("${client.suspendReadString()}${client.suspendReadString()}".also { println(it) }, "12")
            }
        }

        val client = Netlius.client("127.0.0.1", 12345)
        val packet = Packet().string("1").string("2")

        runBlocking {
            client.queueAndFlush(packet)
            delay(10000)
        }

        server.stop()
    }

    @Test
    fun `client prepend message test`() {

        val server = Netlius.server("127.0.0.1", 12345)
        var succeeded = false

        server.onConnect { client ->
            while (client.channel.isOpen) {
                assertEquals("${client.suspendReadString()}${client.suspendReadString()}${client.suspendReadString()}", "123")
                succeeded = true
            }
        }

        val client = Netlius.client("127.0.0.1", 12345)

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

        server.stop()
    }


    @Test
    fun `attack of the client part 1 season 1`() = runBlocking {

        val server = Netlius.server("127.0.0.1", 12345)

        server.onConnect { client ->
            println(measureTimeMillis {
                repeat(7_000) {
                    assertEquals(client.suspendReadString(), "Meow")
                }
            })
        }

        repeat(100_000) {

            val client = Netlius.clientSuspending("127.0.0.1", 12345)
            val meowPacket = Packet().string("Meow")

            //client.queueAndFlush(meowPacket)

            (1..7000).map {
                async(Netlius.threadPoolDispatcher, CoroutineStart.LAZY) {
                    client.queueAndFlush(meowPacket)
                }
            }.awaitAll()
        }

        delay(10_000)

        server.stop()
    }

    @Test
    fun `attack of the clients part 2 season 4`(): Unit = runBlocking {

        val server = Netlius.server("127.0.0.1", 12345)
        val count = atomic(0)

        server.onConnect { client ->

            assertEquals(client.suspendReadString(), "Meow")
            assertEquals(client.suspendReadString(), "Test")

            count += 1
        }

        val packet = Packet().string("Meow").string("Test")

        (1..1_000).map {
            async(Netlius.threadPoolDispatcher, CoroutineStart.LAZY) {
                val client = Netlius.clientSuspending("127.0.0.1", 12345)
                client.queueAndFlush(packet)
            }
        }.awaitAll()

        while (count.value != 1_000) {
            Thread.onSpinWait()
        }

        server.stop()
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
                client.suspendReadByte()
            }
            catch (ex: Exception) {
                // Ignore
            }
        }

        assert(!client.channel.isOpen)

    }

    @Test
    fun `server client auto close test`() {


        val server = Netlius.server("127.0.0.1", 12345, defaultTimeoutMS = 1_000)
        val client = Netlius.client("127.0.0.1", 12345)

        var clientDisconnect = false
        var serverClientDisconnect = false

        client.onDisconnect {
            clientDisconnect = true
        }

        server.onConnect {
            client.close()
            GlobalScope.launch {
                it.suspendReadByte()
            }
            it.onDisconnect {
                serverClientDisconnect = true
            }
        }

        runBlocking {
            delay(2_000)
        }

        assert(!client.channel.isOpen)
        assert(clientDisconnect)
        assert(serverClientDisconnect)

        server.stop()
    }

    @Test
    fun `read from closed client`() {

        val server = Netlius.server("127.0.0.1", 12345, defaultTimeoutMS = Long.MAX_VALUE)
        val client = Netlius.client("127.0.0.1", 12345, Long.MAX_VALUE)

        server.onConnect {
            println(it.suspendReadInt())
        }

        client.close()

        runBlocking {
            delay(10000)
            Netlius.client("127.0.0.1", 12345, Long.MAX_VALUE).queueAndFlush(Packet().int(1))
            delay(10000)
        }

        server.stop()
    }

    @Test
    fun `long read`() {

        val server = Netlius.server("127.0.0.1", 12345, defaultTimeoutMS = Long.MAX_VALUE)
        val client = Netlius.client("127.0.0.1", 12345, Long.MAX_VALUE)

        server.onConnect {
            delay(30000)
            it.queueAndFlush(Packet().boolean(true))
        }

        runBlocking {
            println(client.suspendReadBoolean())
        }

        server.stop()
    }


    // TODO: Test with a longer read timeout when you make that a configurable option... TSSK TSSSK
    /*
    @Test
    fun `slow client reader test`(): Unit = runBlocking {

        val server = Netlius.server("127.0.0.1", 12345)

        server.onConnect { client ->

            repeat(1_000_000) {
                delay(10)
                assertEquals(client.readLong(), 10)
            }

            exitProcess(0)
        }

        val client = Netlius.client("127.0.0.1", 12345)
        val packet = Packet().long(10)

        repeat (1_000_000) {
            client.queueAndFlush(packet)
        }

        println("All sent")

        delay(Long.MAX_VALUE)
    }
    */


    /*
    @Test
    fun `10 million connections?!`(): Unit = runBlocking {

        val server = Netlius.server("127.0.0.1", 12345)
        val count = atomic(0)

        server.onConnect { client ->
            client.readByte()
            println(count)
            count += 1
        }

        val blankPacket = Packet().byte(0)

        (1..10_000_000).map {
            async(Netlius.threadPoolDispatcher, CoroutineStart.LAZY) {
                Netlius.clientSuspending("127.0.0.1", 12345).queueAndFlush(blankPacket)
            }
        }.awaitAll()

        while (count.value != 1_000) {
            Thread.onSpinWait()
        }
    }
    */

}
*/