package me.muksc.tacztweaks.network.message
import net.neoforged.neoforge.network.handling.IPayloadContext

import net.neoforged.neoforge.items.ItemHandlerHelper
import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.item.IGun
import com.tacz.guns.api.item.builder.AmmoItemBuilder
import com.tacz.guns.resource.pojo.data.gun.Bolt
import com.tacz.guns.resource.pojo.data.gun.FeedType
import io.netty.buffer.Unpooled
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.core.Context.hasInfiniteAmmo
import me.muksc.tacztweaks.core.StackSplitter
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.network.codec.StreamCodec
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.resources.Identifier
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

class ClientMessagePlayerUnload private constructor(@Suppress("UNUSED_PARAMETER") buf: FriendlyByteBuf) : CustomPacketPayload {
    fun write(out: FriendlyByteBuf) = Unit

    override fun type(): CustomPacketPayload.Type<out CustomPacketPayload> = TYPE

    companion object {
        val TYPE = CustomPacketPayload.Type<ClientMessagePlayerUnload>(
            Identifier.fromNamespaceAndPath(TaCZTweaks.MOD_ID, "client_player_unload")
        )
        val CODEC: StreamCodec<FriendlyByteBuf, ClientMessagePlayerUnload> = StreamCodec.ofMember(
            ClientMessagePlayerUnload::write,
            ::ClientMessagePlayerUnload
        )

        fun create(): ClientMessagePlayerUnload = ClientMessagePlayerUnload(FriendlyByteBuf(Unpooled.buffer()))

        fun handle(msg: ClientMessagePlayerUnload, ctx: IPayloadContext) {
            if (!Config.Gun.allowUnload()) return
            ctx.enqueueWork {
                val player = ctx.player() as? ServerPlayer ?: return@enqueueWork
                if (!player.isAlive || player.isRemoved) return@enqueueWork
                val gunStack = player.mainHandItem
                if (player.inventory.hasInfiniteAmmo(gunStack)) return@enqueueWork

                val gun = IGun.getIGunOrNull(gunStack) ?: return@enqueueWork
                val gunId = gun.getGunId(gunStack)
                val index = TimelessAPI.getCommonGunIndex(gunId).orElse(null) ?: return@enqueueWork
                val gunData = index.getGunData()
                val ammoCount = gun.getCurrentAmmoCount(gunStack)
                if (ammoCount !in 0..StackSplitter.MAX_UNLOAD_AMMO) return@enqueueWork

                val unloadChamber = Config.Gun.unloadBulletInBarrel() &&
                    gunData.getBolt() != Bolt.OPEN_BOLT && gun.hasBulletInBarrel(gunStack)
                val inventoryFeed = gun.useInventoryAmmo(gunStack)
                val dummyAmmo = gun.useDummyAmmo(gunStack)
                val fuelFeed = gunData.getReloadData().getType() == FeedType.FUEL

                if (fuelFeed) {
                    if (!inventoryFeed && ammoCount > 0) gun.setCurrentAmmoCount(gunStack, 0)
                    if (unloadChamber) gun.setBulletInBarrel(gunStack, false)
                    return@enqueueWork
                }

                if (dummyAmmo) {
                    val magazineRounds = if (inventoryFeed) 0 else ammoCount
                    val returned = magazineRounds + if (unloadChamber) 1 else 0
                    if (returned > 0) {
                        gun.addDummyAmmoAmount(gunStack, returned)
                        if (magazineRounds > 0) gun.setCurrentAmmoCount(gunStack, 0)
                        if (unloadChamber) gun.setBulletInBarrel(gunStack, false)
                    }
                    return@enqueueWork
                }

                val physicalRounds = (if (inventoryFeed) 0 else ammoCount) + if (unloadChamber) 1 else 0
                if (physicalRounds <= 0) return@enqueueWork
                val ammoId = gunData.getAmmoId()
                val ammoIndex = TimelessAPI.getCommonAmmoIndex(ammoId).orElse(null) ?: return@enqueueWork
                val chunks = StackSplitter.split(physicalRounds, ammoIndex.getStackSize()) ?: return@enqueueWork

                val returnedStacks = chunks.map { count ->
                    AmmoItemBuilder.create().setId(ammoId).setCount(count).build()
                }
                returnedStacks.forEach { ItemHandlerHelper.giveItemToPlayer(player, it) }
                if (!inventoryFeed && ammoCount > 0) gun.setCurrentAmmoCount(gunStack, 0)
                if (unloadChamber) gun.setBulletInBarrel(gunStack, false)
            }
        }
    }
}
