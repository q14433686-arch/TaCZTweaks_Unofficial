package me.muksc.tacztweaks.network.message

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.item.IGun
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.mixininterface.gun.SlideDataHolder
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

class ClientMessagePlayerShouldSlide private constructor(private val shouldSlide: Boolean) : CustomPacketPayload {
    constructor(buf: FriendlyByteBuf) : this(buf.readBoolean())

    fun write(out: FriendlyByteBuf) {
        out.writeBoolean(shouldSlide)
    }

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ClientMessagePlayerShouldSlide>(
            Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "client_player_should_slide")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, ClientMessagePlayerShouldSlide> = StreamCodec.ofMember(
            ClientMessagePlayerShouldSlide::write,
            { buf -> ClientMessagePlayerShouldSlide(buf) }
        )

        @JvmStatic
        fun create(shouldSlide: Boolean): ClientMessagePlayerShouldSlide = ClientMessagePlayerShouldSlide(shouldSlide)

        private const val REQUEST_TIMEOUT_TICKS = 40

        fun handle(msg: ClientMessagePlayerShouldSlide, server: MinecraftServer, player: ServerPlayer?, responseSender: PacketSender) {
            server.execute {
                if (player == null) return@execute
                val holder = player as SlideDataHolder
                val accepted = msg.shouldSlide && canRequestSlide(player)
                holder.`tacztweaks$setShouldSlide`(accepted)
                holder.`tacztweaks$setSlideRequestExpiry`(
                    if (accepted) player.tickCount + REQUEST_TIMEOUT_TICKS else 0
                )
            }
        }

        @JvmStatic
        fun validateServerState(player: ServerPlayer) {
            val holder = player as SlideDataHolder
            if (!holder.`tacztweaks$getShouldSlide`()) return
            if (holder.`tacztweaks$getSlideRequestExpiry`() < player.tickCount || !canRequestSlide(player)) {
                holder.`tacztweaks$setShouldSlide`(false)
                holder.`tacztweaks$setSlideRequestExpiry`(0)
            }
        }

        /** Server-visible eligibility; the packet is only a short-lived input request. */
        @JvmStatic
        fun canRequestSlide(player: ServerPlayer): Boolean {
            if (!Config.Tweaks.betterGunTilt() || !player.isAlive || player.isRemoved) return false
            if (!IGun.mainHandHoldGun(player)) return false
            val gun = IGun.getIGunOrNull(player.mainHandItem) ?: return false
            val index = TimelessAPI.getCommonGunIndex(gun.getGunId(player.mainHandItem)).orElse(null)
                ?: return false
            return index.getGunData().canSlide()
        }
    }
}
