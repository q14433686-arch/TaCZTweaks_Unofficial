package me.muksc.tacztweaks.config

import net.minecraft.client.Minecraft
import net.minecraft.server.permissions.Permissions
import net.minecraft.world.entity.player.Player

object ConfigManager {
    var syncedWithServer = false

    fun canUpdateServerConfig(): Boolean {
        val player = Minecraft.getInstance().player ?: return false
        return canUpdateServerConfig(player)
    }

    /** Permission state is synchronised onto LocalPlayer in 26.1.2; no ServerPlayer cast. */
    fun canUpdateServerConfig(player: Player): Boolean =
        player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
}
