package me.muksc.tacztweaks.network.message

import com.tacz.guns.api.item.IGun
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.network.NetworkHandler
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.handling.IPayloadContext
import net.neoforged.neoforge.server.ServerLifecycleHooks
import java.util.ArrayDeque
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class ClientMessageBroadcastSound(
    private val soundName: Identifier,
    private val volume: Float,
    private val pitch: Float,
    private val distance: Int
) : CustomPacketPayload {
    constructor(buf: FriendlyByteBuf) : this(
        buf.readIdentifier(),
        buf.readFloat(),
        buf.readFloat(),
        buf.readInt()
    )

    fun write(out: FriendlyByteBuf) {
        out.writeIdentifier(soundName)
        out.writeFloat(volume)
        out.writeFloat(pitch)
        out.writeInt(distance)
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ClientMessageBroadcastSound>(
            Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "client_broadcast_sound")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, ClientMessageBroadcastSound> = StreamCodec.ofMember(
            ClientMessageBroadcastSound::write,
            { buf -> ClientMessageBroadcastSound(buf) }
        )

        private const val MAX_DISTANCE = 96
        private const val MAX_SOUNDS_PER_SECOND = 16
        private val recentSounds = ConcurrentHashMap<UUID, ArrayDeque<Long>>()

        fun handle(msg: ClientMessageBroadcastSound, ctx: IPayloadContext) {
            ctx.enqueueWork {
                val player = ctx.player() as? ServerPlayer ?: return@enqueueWork
                val server = ServerLifecycleHooks.getCurrentServer() ?: return@enqueueWork
                if (!Config.Tweaks.audibleFirstPersonGunSounds()) return@enqueueWork
                if (!player.isAlive || player.isRemoved || !IGun.mainHandHoldGun(player)) return@enqueueWork
                val gun = IGun.getIGunOrNull(player.mainHandItem) ?: return@enqueueWork
                val gunId = gun.getGunId(player.mainHandItem)
                val displayId = gun.getGunDisplayId(player.mainHandItem)
                val allowedNamespaces = setOf(
                    "tacz",
                    TaCZTweaks.MOD_ID,
                    gunId.namespace,
                    displayId.namespace
                )
                if (msg.soundName.namespace !in allowedNamespaces) return@enqueueWork
                if (msg.distance !in 1..MAX_DISTANCE || !msg.volume.isFinite() || msg.volume !in 0.0F..4.0F ||
                    !msg.pitch.isFinite() || msg.pitch !in 0.01F..4.0F || !allowSound(player.uuid)) return@enqueueWork

                val pos = player.position()
                val distanceSqr = msg.distance.toDouble() * msg.distance.toDouble()
                server.playerList.players
                    .asSequence()
                    .filter { it.level().dimension() == player.level().dimension() }
                    .filter { it.id != player.id }
                    .filter { it.distanceToSqr(pos.x, pos.y, pos.z) < distanceSqr }
                    .forEach {
                        NetworkHandler.sendS2C(
                            it,
                            ServerMessageBroadcastSound(player.id, msg.soundName, msg.volume, msg.pitch, msg.distance)
                        )
                    }
            }
        }

        @JvmStatic
        fun clearPlayer(player: UUID) {
            recentSounds.remove(player)
        }

        @JvmStatic
        fun clearAll() {
            recentSounds.clear()
        }

        private fun allowSound(player: UUID): Boolean {
            val now = System.currentTimeMillis()
            val queue = recentSounds.computeIfAbsent(player) { ArrayDeque() }
            synchronized(queue) {
                while (queue.isNotEmpty() && queue.first < now - 1_000L) queue.removeFirst()
                if (queue.size >= MAX_SOUNDS_PER_SECOND) return false
                queue.addLast(now)
                return true
            }
        }
    }
}
