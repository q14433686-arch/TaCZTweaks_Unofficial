package me.muksc.tacztweaks.core.network

import me.muksc.tacztweaks.core.codec.StreamCodec
import net.minecraft.network.RegistryFriendlyByteBuf
import net.minecraft.network.protocol.common.custom.CustomPacketPayload as MCCustomPacketPayload

interface CustomPacketPayload<T : CustomPacketPayload<T>> : MCCustomPacketPayload {
    fun self(): T

    override fun type(): CustomPacketPayloadType<T>

    fun codec(): StreamCodec<in RegistryFriendlyByteBuf, T>
}

typealias CustomPacketPayloadType<T> = MCCustomPacketPayload.Type<T>
