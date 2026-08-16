package me.muksc.tacztweaks.client.input

import com.mojang.blaze3d.platform.InputConstants
import com.tacz.guns.api.entity.IGunOperator
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.config.Config
import net.minecraft.client.ToggleKeyMapping
import net.minecraft.client.player.LocalPlayer

object ReduceSensitivityKey {
    @JvmField
    //? if >=1.21.11 {
    /*val KEY = ToggleKeyMapping(
        TaCZTweaks.key("reduceSensitivity"),
        InputConstants.UNKNOWN.value,
        ModKeyCategory.VALUE,
        { Config.KeyActions.ReduceSensitivity.type().isToggle() },
        false
    )
    *///?} else {
    val KEY = ToggleKeyMapping(
        TaCZTweaks.key("reduceSensitivity"),
        InputConstants.UNKNOWN.value,
        ModKeyCategory.VALUE
    ) { Config.KeyActions.ReduceSensitivity.type().isToggle() }
    //?}

    @JvmStatic
    fun getCurrentSensitivityMultiplier(player: LocalPlayer): Double {
        val sensitivityMultiplier = Config.KeyActions.ReduceSensitivity.sensitivityMultiplier()
        if (!Config.KeyActions.ReduceSensitivity.disableWhileAiming()) return sensitivityMultiplier
        val aimingProgress = IGunOperator.fromLivingEntity(player).synAimingProgress
        return 1.0 + (sensitivityMultiplier - 1.0) * (1.0 - aimingProgress)
    }
}