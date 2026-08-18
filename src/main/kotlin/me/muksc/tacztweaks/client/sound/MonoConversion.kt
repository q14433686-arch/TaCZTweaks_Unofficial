package me.muksc.tacztweaks.client.sound

import me.muksc.tacztweaks.config.Config
import net.minecraft.resources.Identifier
import java.nio.ByteBuffer
import java.util.ArrayDeque
import javax.sound.sampled.AudioFormat

/** Request-scoped PCM conversion helpers for betterMonoConversion. */
object MonoConversion {
    private val pendingRequests = ThreadLocal.withInitial {
        HashMap<Identifier, ArrayDeque<Boolean>>()
    }
    private val activeConversion = ThreadLocal<Identifier?>()

    fun request(id: Identifier, mono: Boolean) {
        pendingRequests.get().computeIfAbsent(id) { ArrayDeque() }.addLast(mono)
    }

    fun consumeMonoRequest(id: Identifier): Boolean {
        val requests = pendingRequests.get()
        val queue = requests[id]
        val mono = if (queue == null || queue.isEmpty()) false else queue.removeFirst()
        if (queue == null || queue.isEmpty()) requests.remove(id)
        return Config.Tweaks.betterMonoConversion() && mono
    }

    fun beginConversion(id: Identifier) {
        activeConversion.set(id)
    }

    fun endConversion() {
        activeConversion.remove()
    }

    fun clear() {
        pendingRequests.remove()
        activeConversion.remove()
    }

    fun shouldConvert(format: AudioFormat, id: Identifier): Boolean {
        if (activeConversion.get() != id || format.channels != 2) return false
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
