package me.muksc.tacztweaks.network

import me.muksc.tacztweaks.network.message.ClientMessageBroadcastSound
import me.muksc.tacztweaks.network.message.ClientMessagePlayerShouldSlide
import me.muksc.tacztweaks.network.message.ClientMessagePlayerUnload
import me.muksc.tacztweaks.network.message.ClientMessageSyncConfig
import me.muksc.tacztweaks.compat.soundphysics.network.message.ServerMessageAirspaceSounds
import me.muksc.tacztweaks.compat.soundphysics.network.message.ServerMessageSoundPhysicsRequired
import me.muksc.tacztweaks.network.message.ServerMessageBroadcastSound
import me.muksc.tacztweaks.network.message.ServerMessageSyncConfig
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.server.MinecraftServer
import net.minecraft.server.level.ServerPlayer

object NetworkHandler {
    fun registerPayloads() {
        PayloadTypeRegistry.serverboundPlay().register(ClientMessageSyncConfig.TYPE, ClientMessageSyncConfig.CODEC)
        PayloadTypeRegistry.serverboundPlay().register(ClientMessagePlayerUnload.TYPE, ClientMessagePlayerUnload.CODEC)
        PayloadTypeRegistry.serverboundPlay().register(ClientMessageBroadcastSound.TYPE, ClientMessageBroadcastSound.CODEC)
        PayloadTypeRegistry.serverboundPlay().register(ClientMessagePlayerShouldSlide.TYPE, ClientMessagePlayerShouldSlide.CODEC)
        PayloadTypeRegistry.clientboundPlay().register(ServerMessageSyncConfig.TYPE, ServerMessageSyncConfig.CODEC)
        PayloadTypeRegistry.clientboundPlay().register(ServerMessageBroadcastSound.TYPE, ServerMessageBroadcastSound.CODEC)
        PayloadTypeRegistry.clientboundPlay().register(ServerMessageAirspaceSounds.TYPE, ServerMessageAirspaceSounds.CODEC)
        PayloadTypeRegistry.clientboundPlay().register(ServerMessageSoundPhysicsRequired.TYPE, ServerMessageSoundPhysicsRequired.CODEC)
    }

    fun registerServer() {
        registerPayloads()
        ServerPlayNetworking.registerGlobalReceiver(ClientMessageSyncConfig.TYPE) { msg, ctx ->
            ClientMessageSyncConfig.handle(msg, ctx.server(), ctx.player(), ctx.responseSender())
        }
        ServerPlayNetworking.registerGlobalReceiver(ClientMessagePlayerUnload.TYPE) { msg, ctx ->
            ClientMessagePlayerUnload.handle(msg, ctx.server(), ctx.player(), ctx.responseSender())
        }
        ServerPlayNetworking.registerGlobalReceiver(ClientMessageBroadcastSound.TYPE) { msg, ctx ->
            ClientMessageBroadcastSound.handle(msg, ctx.server(), ctx.player(), ctx.responseSender())
        }
        ServerPlayNetworking.registerGlobalReceiver(ClientMessagePlayerShouldSlide.TYPE) { msg, ctx ->
            ClientMessagePlayerShouldSlide.handle(msg, ctx.server(), ctx.player(), ctx.responseSender())
        }
    }

    fun registerClient() {
        ClientPlayNetworking.registerGlobalReceiver(ServerMessageSyncConfig.TYPE) { msg, ctx ->
            ServerMessageSyncConfig.handle(msg, ctx.client())
        }
        ClientPlayNetworking.registerGlobalReceiver(ServerMessageBroadcastSound.TYPE) { msg, ctx ->
            ServerMessageBroadcastSound.handle(msg, ctx.client())
        }
        ClientPlayNetworking.registerGlobalReceiver(ServerMessageAirspaceSounds.TYPE) { msg, ctx ->
            ServerMessageAirspaceSounds.handle(msg, ctx.client())
        }
        ClientPlayNetworking.registerGlobalReceiver(ServerMessageSoundPhysicsRequired.TYPE) { msg, ctx ->
            ServerMessageSoundPhysicsRequired.handle(msg, ctx.client())
        }
    }

    fun sendC2S(payload: CustomPacketPayload) {
        ClientPlayNetworking.send(payload)
    }

    fun sendPlayerUnload() {
        ClientPlayNetworking.send(ClientMessagePlayerUnload)
    }

    fun sendS2C(player: ServerPlayer, payload: CustomPacketPayload) {
        ServerPlayNetworking.send(player, payload)
    }

    fun sendSyncConfig(player: ServerPlayer) {
        ServerPlayNetworking.send(player, ServerMessageSyncConfig.create())
    }

    fun sendSyncConfigAll(server: MinecraftServer) {
        val payload = ServerMessageSyncConfig.create()
        server.playerList.players.forEach { ServerPlayNetworking.send(it, payload) }
    }
}
