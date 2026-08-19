package me.muksc.tacztweaks.client.sound

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat

class MonoConversionTest {
    @Test
    fun `averages signed little-endian 16-bit stereo frames`() {
        val format = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48_000F, 16, 2, 4, 48_000F, false)
        val source = ByteBuffer.allocateDirect(8).order(ByteOrder.LITTLE_ENDIAN)
            .putShort(1_000.toShort()).putShort((-1_000).toShort())
            .putShort(30_000.toShort()).putShort(10_000.toShort())
            .flip()

        val mono = MonoConversion.convertData(source, format).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(0, mono.short.toInt())
        assertEquals(20_000, mono.short.toInt())
    }

    @Test
    fun `averages signed 8-bit stereo without byte overflow`() {
        val format = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 48_000F, 8, 2, 2, 48_000F, false)
        val source = ByteBuffer.allocateDirect(4)
            .put((-128).toByte()).put(127.toByte())
            .put(100.toByte()).put(20.toByte())
            .flip()

        val mono = MonoConversion.convertData(source, format)
        assertEquals(0, mono.get().toInt())
        assertEquals(60, mono.get().toInt())
    }

    @Test
    fun `averages unsigned 8-bit stereo in unsigned domain`() {
        val format = AudioFormat(AudioFormat.Encoding.PCM_UNSIGNED, 48_000F, 8, 2, 2, 48_000F, false)
        val source = ByteBuffer.allocateDirect(4)
            .put(0.toByte()).put(255.toByte())
            .put(100.toByte()).put(200.toByte())
            .flip()

        val mono = MonoConversion.convertData(source, format)
        assertEquals(127, mono.get().toInt() and 0xFF)
        assertEquals(150, mono.get().toInt() and 0xFF)
    }
}
