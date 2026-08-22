package me.muksc.tacztweaks.config

import net.minecraft.client.Minecraft
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.permissions.Permissions

object ConfigManager {
    var syncedWithServer = false

    fun canUpdateServerConfig(): Boolean {
        val minecraft = Minecraft.getInstance()
        val player = minecraft.player ?: return false
        return canUpdateServerConfig(player)
    }

    fun canUpdateServerConfig(player: net.minecraft.world.entity.player.Player): Boolean {
        val serverPlayer = player as? ServerPlayer ?: return false
        return serverPlayer.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
    }
}
