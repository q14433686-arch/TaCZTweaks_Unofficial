package me.muksc.tacztweaks.network

import me.muksc.tacztweaks.compat.soundphysics.network.message.ServerMessageAirspaceSounds
import me.muksc.tacztweaks.compat.soundphysics.network.message.ServerMessageSoundPhysicsRequired
import me.muksc.tacztweaks.network.message.ClientMessageBroadcastSound
import me.muksc.tacztweaks.network.message.ClientMessagePlayerShouldSlide
import me.muksc.tacztweaks.network.message.ClientMessagePlayerUnload
import me.muksc.tacztweaks.network.message.ClientMessageSyncConfig
import me.muksc.tacztweaks.network.message.ServerMessageBroadcastSound
import me.muksc.tacztweaks.network.message.ServerMessageSyncConfig
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer
import net.neoforged.neoforge.network.PacketDistributor
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent

/**
 * NeoForge 网络层（Fabric 三段合一为一次 RegisterPayloadHandlersEvent 注册）。
 */
object NetworkHandler {
    private const val VERSION = "1"

    fun register(event: RegisterPayloadHandlersEvent) {
        val registrar = event.registrar(VERSION)
        registrar.playToServer(ClientMessageSyncConfig.TYPE, ClientMessageSyncConfig.CODEC, ClientMessageSyncConfig::handle)
        registrar.playToServer(ClientMessagePlayerUnload.TYPE, ClientMessagePlayerUnload.CODEC, ClientMessagePlayerUnload::handle)
        registrar.playToServer(ClientMessageBroadcastSound.TYPE, ClientMessageBroadcastSound.CODEC, ClientMessageBroadcastSound::handle)
        registrar.playToServer(ClientMessagePlayerShouldSlide.TYPE, ClientMessagePlayerShouldSlide.CODEC, ClientMessagePlayerShouldSlide::handle)
        registrar.playToClient(ServerMessageSyncConfig.TYPE, ServerMessageSyncConfig.CODEC, ServerMessageSyncConfig::handle)
        registrar.playToClient(ServerMessageBroadcastSound.TYPE, ServerMessageBroadcastSound.CODEC, ServerMessageBroadcastSound::handle)
        registrar.playToClient(ServerMessageAirspaceSounds.TYPE, ServerMessageAirspaceSounds.CODEC, ServerMessageAirspaceSounds::handle)
        registrar.playToClient(ServerMessageSoundPhysicsRequired.TYPE, ServerMessageSoundPhysicsRequired.CODEC, ServerMessageSoundPhysicsRequired::handle)
    }

    fun sendC2S(payload: CustomPacketPayload) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(payload)
    }

    fun sendPlayerUnload() {
        sendC2S(ClientMessagePlayerUnload.create())
    }

    fun sendS2C(player: ServerPlayer, payload: CustomPacketPayload) {
        PacketDistributor.sendToPlayer(player, payload)
    }

    fun sendSyncConfig(player: ServerPlayer) {
        sendS2C(player, ServerMessageSyncConfig.create())
    }

    fun sendSyncConfigAll(server: MinecraftServer) {
        val payload = ServerMessageSyncConfig.create()
        server.playerList.players.forEach { PacketDistributor.sendToPlayer(it, payload) }
    }
}
