package me.muksc.tacztweaks.client.sound

import me.muksc.tacztweaks.config.Config
import net.minecraft.resources.Identifier
import java.nio.ByteBuffer
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import javax.sound.sampled.AudioFormat

/** PCM conversion helpers for betterMonoConversion. */
object MonoConversion {
    /**
     * TaCZ routes mono world sounds through the same logical sound id/path that Minecraft caches.
     * Once a path is requested as mono we keep remembering it until disconnect/resource reset,
     * mirroring the upstream cache behaviour while avoiding mutable data on Identifier records.
     */
    private val monoPaths = Collections.newSetFromMap(ConcurrentHashMap<Identifier, Boolean>())

    fun request(id: Identifier, mono: Boolean) {
        if (Config.Tweaks.betterMonoConversion() && mono) monoPaths.add(id)
    }

    fun clear() {
        monoPaths.clear()
    }

    fun shouldConvert(format: AudioFormat, id: Identifier): Boolean {
        if (!Config.Tweaks.betterMonoConversion() || id !in monoPaths || format.channels != 2) return false
        return when (format.sampleSizeInBits) {
            16 -> format.encoding == AudioFormat.Encoding.PCM_SIGNED
            8 -> format.encoding == AudioFormat.Encoding.PCM_SIGNED || format.encoding == AudioFormat.Encoding.PCM_UNSIGNED
            else -> false
        }
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
                val left = if (format.encoding == AudioFormat.Encoding.PCM_UNSIGNED) input.get().toInt() and 0xFF else input.get().toInt()
                val right = if (format.encoding == AudioFormat.Encoding.PCM_UNSIGNED) input.get().toInt() and 0xFF else input.get().toInt()
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
        if (format.frameSize > 0) format.frameSize / 2 else format.frameSize,
        format.frameRate,
        format.isBigEndian,
        format.properties()
    )
}
