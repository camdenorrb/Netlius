package me.camdenorrb.netlius.net

import java.nio.ByteBuffer

// TODO: Add a way to add FileInputStream - No, just use multiple packets
class Packet {

    var size = 0
        private set

    @PublishedApi
    internal var isPrepending = false

    @PublishedApi
    internal val writeQueue = mutableListOf<Any>()

    @PublishedApi
    internal val prependWriteQueue = mutableListOf<Any>()


    // Numbers

    fun byte(byte: Byte): Packet {
        return addWriteValue(Byte.SIZE_BYTES, byte)
    }

    fun boolean(boolean: Boolean): Packet {
        return byte(if (boolean) 1 else 0)
    }

    fun bytes(bytes: ByteArray): Packet {
        return addWriteValue(bytes.size * Byte.SIZE_BYTES, bytes)
    }

    fun short(short: Short): Packet {
        return addWriteValue(Short.SIZE_BYTES, short)
    }

    fun int(int: Int): Packet {
        return addWriteValue(Int.SIZE_BYTES, int)
    }

    fun long(long: Long): Packet {
        return addWriteValue(Long.SIZE_BYTES, long)
    }

    fun float(float: Float): Packet {
        return addWriteValue(Float.SIZE_BYTES, float)
    }

    fun double(double: Double): Packet {
        return addWriteValue(Double.SIZE_BYTES, double)
    }

    fun byteBuffer(byteBuffer: ByteBuffer): Packet {
        return addWriteValue(byteBuffer.remaining(), byteBuffer)
    }

    fun string(string: String): Packet {

        val bytes = string.encodeToByteArray()

        short(bytes.size.toShort())
        return bytes(bytes)
    }


    private fun addWriteValue(size: Int, value: Any): Packet {

        this.size += size

        if (isPrepending) {
            prependWriteQueue.add(value)
        }
        else {
            writeQueue.add(value)
        }

        return this
    }

    fun writeToBuffer(buffer: ByteBuffer) {
        writeQueue.forEach { value ->
            when (value) {
                is Byte -> buffer.put(value)
                is ByteArray -> buffer.put(value)
                is Short -> buffer.putShort(value)
                is Int -> buffer.putInt(value)
                is Long -> buffer.putLong(value)
                is Float -> buffer.putFloat(value)
                is Double -> buffer.putDouble(value)
                is ByteBuffer -> buffer.put(value)
            }
        }
    }

    inline fun prepend(block: Packet.() -> Unit): Packet {

        isPrepending = true
        block()
        isPrepending = false

        writeQueue.addAll(0, prependWriteQueue)
        prependWriteQueue.clear()

        return this
    }



    //data class WriteValue(val size: Int, val value: Any)

    /*
    data class WriteTask(val size: Int, val block: (ByteBuffer) -> Unit) {

        operator fun invoke(byteBuffer: ByteBuffer) {
            block(byteBuffer)
        }

    }
    */



    // Extension function for varint in the actual MCServer impl

    /**
     * Packet().string("Meow").prepend {
     *   int(size)
     *   int(id)
     * }
     */
}