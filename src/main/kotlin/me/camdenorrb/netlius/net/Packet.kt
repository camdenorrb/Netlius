package me.camdenorrb.netlius.net

import java.nio.ByteBuffer

// TODO: Add a way to add FileInputStream - No, just use multiple packets
class Packet {

    var size = 0
        private set

    @PublishedApi
    internal var isPrepending = false

    @PublishedApi
    internal val writeQueue = mutableListOf<WriteTask>()

    @PublishedApi
    internal val prependWriteQueue = mutableListOf<WriteTask>()


    // Numbers

    fun byte(byte: Byte): Packet {
        return addWriteTask(Byte.SIZE_BYTES) {
            it.put(byte)
        }
    }

    fun boolean(boolean: Boolean): Packet {
        return byte(if (boolean) 1 else 0)
    }

    fun bytes(bytes: ByteArray): Packet {
        return addWriteTask(bytes.size * Byte.SIZE_BYTES) {
            it.put(bytes)
        }
    }

    fun short(short: Short): Packet {
        return addWriteTask(Short.SIZE_BYTES) {
            it.putShort(short)
        }
    }

    fun int(int: Int): Packet {
        return addWriteTask(Int.SIZE_BYTES) {
            it.putInt(int)
        }
    }

    fun long(long: Long): Packet {
        return addWriteTask(Long.SIZE_BYTES) {
            it.putLong(long)
        }
    }


    fun float(float: Float): Packet {
        return addWriteTask(Float.SIZE_BYTES) {
            it.putFloat(float)
        }
    }

    fun double(double: Double): Packet {
        return addWriteTask(Double.SIZE_BYTES) {
            it.putDouble(double)
        }
    }


    fun addWriteTask(size: Int, block: (ByteBuffer) -> Unit): Packet {

        this.size += size

        if (isPrepending) {
            prependWriteQueue.add(WriteTask(size, block))
        }
        else {
            writeQueue.add(WriteTask(size, block))
        }

        return this
    }


    // Data

    fun string(string: String): Packet {

        val bytes = string.encodeToByteArray()

        short(bytes.size.toShort())
        return bytes(bytes)
    }

    inline fun prepend(block: Packet.() -> Unit): Packet {

        isPrepending = true
        block()
        isPrepending = false

        writeQueue.addAll(0, prependWriteQueue)
        prependWriteQueue.clear()

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