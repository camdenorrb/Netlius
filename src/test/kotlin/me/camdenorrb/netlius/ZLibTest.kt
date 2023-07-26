package me.camdenorrb.netlius

import me.camdenorrb.netlius.ext.decodeToString
import java.nio.ByteBuffer
import java.util.zip.Deflater
import java.util.zip.Inflater
import kotlin.test.Test

internal class ZLibTest {

    @Test
    fun decompressToInput() {

        val deflater = Deflater()
        val inflater = Inflater()

        val plainInput = ByteBuffer.wrap("Meow".encodeToByteArray())

        //println("Here: " + plainInput.decodeToString())

        val output1 = ByteBuffer.allocate(100)
        val output2 = ByteBuffer.allocate(100)

        deflater.setInput(plainInput)
        deflater.finish()
        deflater.deflate(output1, Deflater.FULL_FLUSH)
        deflater.end()

        println(output1)
        println(output2)

        inflater.setInput(output1.flip())
        inflater.inflate(output2)
        inflater.end()

        println(output2.flip().decodeToString())

        //println(output2)
        //println(output2.flip().decodeToString())

        println("Here")

    }

}