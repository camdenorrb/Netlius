# Netlius
A networking library for Kotlin

---

#### Hello world Server + Client example
```kotlin
fun main() {

    val server = Netlius.server("127.0.0.1", 12345)

    server.onConnect { client ->
        while (client.channel.isOpen) {
            println(client.suspendReadString())
        }
    }

    val client = Netlius.client("127.0.0.1", 12345)
    val packet = Packet().string("Hello World")

    runBlocking {
        client.queueAndFlush(packet)
        delay(10000)
    }

    server.stop()
}
```

