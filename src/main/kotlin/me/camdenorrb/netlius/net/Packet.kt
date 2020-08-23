package me.camdenorrb.netlius.net

import java.nio.ByteBuffer

// TODO: Add a way to add FileInputStream - No, just use multiple packets
class Packet {

    @PublishedApi
    internal var size = 0

    @PublishedApi
    internal var isPrepending = false

    @PublishedApi
    internal var prependingIndex = 0

    @PublishedApi
    internal val writeQueue = mutableListOf<WriteTask>()


    // Numbers

    fun byte(byte: Byte): Packet {

        size += Byte.SIZE_BYTES

        return addWriteTask(Byte.SIZE_BYTES) {
            it.put(byte)
        }
    }

    fun short(short: Short): Packet {

        size += Short.SIZE_BYTES

        return addWriteTask(Short.SIZE_BYTES) {
            it.putShort(short)
        }
    }

    fun int(int: Int): Packet {

        size += Int.SIZE_BYTES

        return addWriteTask(Int.SIZE_BYTES) {
            it.putInt(int)
        }
    }

    fun long(long: Long): Packet {

        size += Long.SIZE_BYTES

        return addWriteTask(Long.SIZE_BYTES) {
            it.putLong(long)
        }
    }


    fun float(float: Float): Packet {

        size += Float.SIZE_BYTES

        return addWriteTask(Float.SIZE_BYTES) {
            it.putFloat(float)
        }
    }

    fun double(double: Double): Packet {

        size += Double.SIZE_BYTES

        return addWriteTask(Double.SIZE_BYTES) {
            it.putDouble(double)
        }
    }


    fun addWriteTask(size: Int, block: (ByteBuffer) -> Unit): Packet {

        if (isPrepending) {
            writeQueue.add(prependingIndex++, WriteTask(size, block))
        }
        else {
            writeQueue.add(WriteTask(size, block))
        }

        return this
    }


    // Data

    fun string(string: String): Packet {

        val bytes = string.encodeToByteArray()
        size += bytes.size

        short(bytes.size.toShort())

        return addWriteTask(bytes.size) {
            it.put(bytes)
        }
    }

    inline fun prepend(block: Packet.() -> Unit): Packet {

        isPrepending = true
        block()
        isPrepending = false

        prependingIndex = 0

        return this
    }


    data class WriteTask(val size: Int, val block: (ByteBuffer) -> Unit) {

        operator fun invoke(byteBuffer: ByteBuffer) {
            block(byteBuffer)
        }

    }



    // Extension function for varint in the actual MCServer impl

    /**
     * Packet().string("Meow").prepend {
     *   int(size)
     *   int(id)
     * }
     */
}