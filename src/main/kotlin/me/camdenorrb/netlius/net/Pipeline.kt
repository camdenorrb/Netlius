package me.camdenorrb.netlius.net

/*
import me.camdenorrb.netlius.net.Packet
import java.util.*

class Pipeline {

    val handlers = LinkedList<PacketHandler>()


    operator fun invoke(packet: Packet) {
        handlers.forEach {
            it.onPacket(packet)
        }
    }


    fun addFirst(packetHandler: PacketHandler) {
        handlers.addFirst(packetHandler)
    }

    fun addLast(packetHandler: PacketHandler) {
        handlers.addLast(packetHandler)
    }

    fun addBefore(name: String, packetHandler: PacketHandler) {

        val index = checkNotNull(handlers.indexOfFirst { it.name == name }) {
            "'$name' not found in the pipeline"
        }

        handlers.add(index, packetHandler)
    }

    fun addAfter(name: String, packetHandler: PacketHandler) {

        val index = checkNotNull(handlers.indexOfFirst { it.name == name }) {
            "'$name' not found in the pipeline"
        }

        handlers.add(index + 1, packetHandler)
    }


    data class PacketHandler(val name: String, val onPacket: (Packet) -> Unit)

}
*/