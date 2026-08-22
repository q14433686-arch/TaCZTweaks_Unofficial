package me.muksc.tacztweaks.config

import net.minecraft.server.permissions.Permissions
import net.minecraft.world.entity.player.Player

object ConfigManager {
    var syncedWithServer = false

    /** Permission state is synchronised onto LocalPlayer in 26.2; no ServerPlayer cast. */
    fun canUpdateServerConfig(player: Player): Boolean =
        player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)
}
