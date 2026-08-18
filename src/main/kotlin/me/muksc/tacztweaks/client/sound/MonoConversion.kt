package me.muksc.tacztweaks.client.sound

import me.muksc.tacztweaks.config.Config
import net.minecraft.resources.Identifier
import java.nio.ByteBuffer
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.AudioFormat

/**
 * Side table and pure conversion helpers for [Config.Tweaks.betterMonoConversion].
 *
 * 26.2's [Identifier] is a record, so upstream's attempt to attach a mutable flag to the
 * identifier cannot work. TaCZ's sound object marks the resolved resource path here before
 * [net.minecraft.client.sounds.SoundBufferLibrary] loads it.
 */
object MonoConversion {
    private val monoIds: MutableSet<Identifier> = ConcurrentHashMap.newKeySet()

    fun mark(id: Identifier) {
        monoIds.add(id)
    }

    fun clear() {
        monoIds.clear()
    }

    fun shouldConvert(format: AudioFormat, id: Identifier): Boolean {
        if (!Config.Tweaks.betterMonoConversion()) return false
        if (id !in monoIds) return false
        // Ogg/Vorbis resources used by Minecraft are mono or stereo. Do not guess how to
        // fold an unexpected multi-channel layout.
        if (format.channels != 2) return false
        return format.sampleSizeInBits == 16 || format.sampleSizeInBits == 8
    }

    fun convertData(source: ByteBuffer, format: AudioFormat): ByteBuffer {
        val input = source.duplicate().order(source.order())
        val mono = ByteBuffer.allocateDirect(input.remaining() / 2).order(input.order())
        when (format.sampleSizeInBits) {
            16 -> {
                val stereo = input.asShortBuffer()
                while (stereo.remaining() >= 2) {
                    val left = stereo.get().toInt()
                    val right = stereo.get().toInt()
                    mono.putShort(((left + right) / 2).toShort())
                }
            }
            8 -> while (input.remaining() >= 2) {
                // PCM_SIGNED 8-bit values are signed. Promote before addition to avoid
                // byte overflow, then narrow the averaged result.
                val left = input.get().toInt()
                val right = input.get().toInt()
                mono.put(((left + right) / 2).toByte())
            }
            else -> return source
        }
        return mono.flip()
    }

    fun convertFormat(format: AudioFormat): AudioFormat = AudioFormat(
        format.encoding,
        format.sampleRate,
        format.sampleSizeInBits,
        1,
        format.frameSize / 2,
        format.frameRate,
        format.isBigEndian,
        format.properties()
    )
}
