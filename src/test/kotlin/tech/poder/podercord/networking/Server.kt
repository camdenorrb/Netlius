package tech.poder.podercord.networking

import me.camdenorrb.netlius.Netlius
import me.camdenorrb.netlius.net.Packet


fun main() {

    val server = Netlius.server("127.0.0.1", 12345)

    server.onConnect {
        while(true) {
            it.queueAndFlush(Packet().bytes(it.readBytes(12)))
        }
    }

}