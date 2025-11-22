# Netlius
[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Build Status](https://drone.12oclock.dev/api/badges/camdenorrb/Netlius/status.svg)](https://drone.12oclock.dev/camdenorrb/Netlius)

A networking library for Kotlin.

## Dependency

Add Netlius as a dependency (version auto-updated by release-please):
```kotlin
implementation("dev.twelveoclock:netlius:1.6.2") // x-release-please-version
```

---

#### "Hello world" Server + Client example
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
