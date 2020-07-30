package me.camdenorrb.netlius.net

import java.nio.Buffer
import java.nio.ByteBuffer

class Packet {

    @PublishedApi
    internal var size = 0

    @PublishedApi
    internal var isPrepending = false

    @PublishedApi
    internal val writeQueue = mutableListOf<(ByteBuffer) -> Unit>()


    // Numbers

    fun byte(byte: Byte): Packet {

        size += Byte.SIZE_BYTES

        return addWriteTask {
            it.put(byte)
        }
    }

    fun short(short: Short): Packet {

        size += Short.SIZE_BYTES

        return addWriteTask {
            it.putShort(short)
        }
    }

    fun int(int: Int): Packet {

        size += Int.SIZE_BYTES

        return addWriteTask {
            it.putInt(int)
        }
    }

    fun long(long: Long): Packet {

        size += Long.SIZE_BYTES

        return addWriteTask {
            it.putLong(long)
        }
    }


    fun float(float: Float): Packet {

        size += Float.SIZE_BYTES

        return addWriteTask {
            it.putFloat(float)
        }
    }

    fun double(double: Double): Packet {

        size += Double.SIZE_BYTES

        return addWriteTask {
            it.putDouble(double)
        }
    }


    fun addWriteTask(task: (ByteBuffer) -> Unit): Packet {

        if (isPrepending) {
            writeQueue.add(0, task)
        }
        else {
            writeQueue.add(task)
        }

        return this
    }


    // Data

    fun string(string: String): Packet {

        val bytes = string.encodeToByteArray()
        size += bytes.size

        short(size.toShort())

        return addWriteTask {
            it.put(bytes)
        }
    }


    suspend inline fun compile(block: (ByteBuffer) -> Unit) {

        val byteBuffer = DirectByteBufferPool.take(size)

        writeQueue.forEach {
            it(byteBuffer)
        }

        block(byteBuffer)

        DirectByteBufferPool.give(byteBuffer)
    }


    inline fun prepend(block: Packet.() -> Unit): Packet {

        isPrepending = true
        block()
        isPrepending = false

        return this
    }


    // Extension function for varint in the actual MCServer impl

    /**
     * Packet().string("Meow").prepend {
     *   int(size)
     *   int(id)
     * }
     */
}