package me.muksc.tacztweaks.client.sound

import me.muksc.tacztweaks.config.Config
import net.minecraft.resources.Identifier
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.AudioFormat

/**
 * Side table for [betterMonoConversion].
 *
 * 26.2's [Identifier] is a record, so the upstream "mixin a boolean onto ResourceLocation"
 * trick is gone. We remember which sound ids TaCZ requested as mono and convert the
 * stereo buffer when [net.minecraft.client.sounds.SoundBufferLibrary] loads them.
 */
object MonoConversion {
    private val monoIds: MutableSet<Identifier> = ConcurrentHashMap.newKeySet()

    fun mark(id: Identifier) {
        monoIds.add(id)
    }

    fun isMarked(id: Identifier): Boolean = monoIds.contains(id)

    fun shouldConvert(format: AudioFormat, id: Identifier): Boolean {
        if (!Config.Tweaks.betterMonoConversion()) return false
        if (!isMarked(id)) return false
        if (format.channels == 1) return false
        val bits = format.sampleSizeInBits
        return bits == 16 || bits == 8
    }

    fun convert(data: ByteBuffer, format: AudioFormat): Pair<ByteBuffer, AudioFormat> {
        val bits = format.sampleSizeInBits
        val monoBuffer = ByteBuffer.allocateDirect(data.remaining() / 2)
        monoBuffer.order(data.order())
        if (bits == 16) {
            val stereo = data.asShortBuffer()
            while (stereo.hasRemaining()) {
                val left = stereo.get()
                val right = stereo.get()
                monoBuffer.putShort(((left + right) / 2).toShort())
            }
        } else if (bits == 8) {
            while (data.hasRemaining()) {
                val left = data.get()
                val right = data.get()
                monoBuffer.put(((left + right) / 2).toByte())
            }
        } else {
            return data to format
        }
        monoBuffer.flip()
        val monoFormat = AudioFormat(
            format.encoding,
            format.sampleRate,
            format.sampleSizeInBits,
            1,
            format.frameSize / 2,
            format.frameRate,
            format.isBigEndian,
            format.properties()
        )
        return monoBuffer to monoFormat
    }
}
