package me.muksc.tacztweaks.network.message

import cn.sh1rocu.tacz.util.itemhandler.ItemHandlerHelper
import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.item.IGun
import com.tacz.guns.api.item.builder.AmmoItemBuilder
import com.tacz.guns.resource.pojo.data.gun.Bolt
import com.tacz.guns.resource.pojo.data.gun.FeedType
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.core.Context.hasInfiniteAmmo
import net.fabricmc.fabric.api.networking.v1.PacketSender
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import kotlin.math.min

/** Empty C2S action payload; StreamCodec.unit avoids a fake no-op writer/temporary buffer. */
object ClientMessagePlayerUnload : CustomPacketPayload {
    val TYPE = CustomPacketPayload.Type<ClientMessagePlayerUnload>(
        Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "client_player_unload")
    )
    val CODEC: StreamCodec<FriendlyByteBuf, ClientMessagePlayerUnload> =
        StreamCodec.unit(ClientMessagePlayerUnload)

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    fun handle(msg: ClientMessagePlayerUnload, server: MinecraftServer, player: ServerPlayer?, responseSender: PacketSender) {
        if (!Config.Gun.allowUnload()) return
        server.execute {
            if (player == null) return@execute
            val gunStack = player.mainHandItem
            if (player.inventory.hasInfiniteAmmo(gunStack)) return@execute

            val gun = IGun.getIGunOrNull(gunStack) ?: return@execute
            val gunId = gun.getGunId(gunStack)
            val index = TimelessAPI.getCommonGunIndex(gunId).orElse(null) ?: return@execute
            val gunData = index.getGunData()

            // 弹匣内弹药（背包直读枪不处理，虚拟备弹走原 dropAllAmmo 逻辑）
            val ammoCount = gun.getCurrentAmmoCount(gunStack)
            if (ammoCount > 0 && !gun.useInventoryAmmo(gunStack)) {
                if (gun.useDummyAmmo(gunStack)) {
                    gun.dropAllAmmo(player, gunStack)
                } else {
                    val ammoId = gunData.getAmmoId()
                    if (gunData.getReloadData().getType() != FeedType.FUEL) {
                        val ammoIndex = TimelessAPI.getCommonAmmoIndex(ammoId).orElse(null) ?: return@execute
                        val stackSize = ammoIndex.getStackSize().coerceAtLeast(1)
                        var remaining = ammoCount
                        while (remaining > 0) {
                            val count = min(remaining, stackSize)
                            val ammoItem = AmmoItemBuilder.create().setId(ammoId).setCount(count).build()
                            ItemHandlerHelper.giveItemToPlayer(player, ammoItem)
                            remaining -= count
                        }
                    }
                    gun.setCurrentAmmoCount(gunStack, 0)
                }
            }

            // 枪膛内子弹
            if (Config.Gun.unloadBulletInBarrel()) {
                val boltType = gunData.getBolt()
                val hasInBarrel = gun.hasBulletInBarrel(gunStack) && boltType != Bolt.OPEN_BOLT
                if (hasInBarrel) {
                    val ammoId = gunData.getAmmoId()
                    if (TimelessAPI.getCommonAmmoIndex(ammoId).isPresent) {
                        val ammoItem = AmmoItemBuilder.create().setId(ammoId).setCount(1).build()
                        ItemHandlerHelper.giveItemToPlayer(player, ammoItem)
                        gun.setBulletInBarrel(gunStack, false)
                    }
                }
            }
        }
    }
}
