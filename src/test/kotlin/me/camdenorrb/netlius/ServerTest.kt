package me.camdenorrb.netlius

import me.camdenorrb.netlius.net.Client
import me.camdenorrb.netlius.net.Packet
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit
import kotlin.test.Test

class ServerTest {

    @Test
    fun `rust server test`() {

        val server = Netlius.server("127.0.0.1", 12345)

        server.onConnect {
            println(it.readString())
            it.queueAndFlush(Packet().string("Meow"))
        }

        Thread.sleep(1000000)
    }

    @Test
    fun `a minecraft server list responder`() {

        val server = Netlius.server("127.0.0.1", 25565)

        suspend fun Client.readVarInt(): Int {

            var result  = 0
            var numRead = 0

            while (true) {

                val read = readByte().toInt()
                val value = read and 127

                result = result or (value shl (7 * numRead))
                numRead++

                if (numRead > 5) {
                    throw RuntimeException("VarInt is too big")
                }

                if ((read and 128) == 0) {
                    break
                }
            }

            return result
        }


        fun Packet.varInt(value: Int): Packet {

            var currentValue = value

            do {

                var temp     = currentValue and 127
                currentValue = currentValue ushr 7

                if (currentValue != 0) {
                    temp = temp or 128
                }

                byte(temp.toByte())

            } while (currentValue != 0)

            return this
        }

        fun Packet.mcString(value: String): Packet {

            val stringBytes = value.encodeToByteArray()

            varInt(stringBytes.size)
            bytes(stringBytes)

            return this
        }

        suspend fun Client.readMCString(): String {
            val size = readVarInt()
            return readBytes(size).decodeToString()
        }


        data class PacketHeader(val packetLength: Int, val packetID: Int)

        suspend fun Client.readMCPacketHeader(): PacketHeader {
            return PacketHeader(readVarInt(), readVarInt())
        }

        suspend fun handleNextPacket(client: Client) {

            val header = client.readMCPacketHeader()

            when (header.packetID) {

                // Handshake
                0x00 -> {

                    val protocolVersion = client.readVarInt()
                    val serverAddress = client.readMCString()
                    val serverPort = client.readShort()
                    val nextState = client.readVarInt()

                    val handshakePacket = Packet().mcString(
                        """
                            {
                                "version": {
                                    "name": "1.15.2",
                                    "protocol": 578
                                },
                                "players": {
                                    "max": 100,
                                    "online": 5,
                                    "sample": [
                                        {
                                            "name": "MrKitty_Cat",
                                            "id": "3064ce58-a05d-4783-86aa-c700b3e9fc66"
                                        }
                                    ]
                                },	
                                "description": {
                                    "text": "Hello world"
                                }
                            }
                        """.trimIndent()
                    ).prepend {
                        varInt(0x00)
                    }.prepend {
                        varInt(size)
                    }

                    // Read blank request packet
                    client.readMCPacketHeader()
                    client.queueAndFlush(handshakePacket)

                    val pingPacketHeader = client.readMCPacketHeader()

                    check(pingPacketHeader.packetID == 0x01) {
                        "Invalid ping packet! $pingPacketHeader"
                    }

                    val pingPacket = Packet().long(client.readLong()).prepend {
                        varInt(0x01)
                    }.prepend {
                        varInt(size)
                    }

                    client.queueAndFlush(pingPacket)
                    //client.close()
                }

            }

        }

        server.onConnect { client ->
            while (client.channel.isOpen) {
                handleNextPacket(client)
            }
        }

        // TODO: Make a test like this that makes sure it waited 1 minute before terminating
        (Netlius.threadPoolDispatcher.executor as ExecutorService).awaitTermination(1, TimeUnit.MINUTES)
    }

}