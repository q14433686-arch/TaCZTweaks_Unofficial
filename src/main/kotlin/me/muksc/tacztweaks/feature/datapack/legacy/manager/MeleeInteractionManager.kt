package me.muksc.tacztweaks.feature.datapack.legacy.manager

import com.google.gson.JsonElement
import com.mojang.serialization.JsonOps
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.core.anyOrEmpty
import me.muksc.tacztweaks.core.tacz.GunStack
import me.muksc.tacztweaks.feature.datapack.legacy.MeleeInteraction
import me.muksc.tacztweaks.feature.general.compatibility.LRTacticalManager
import net.minecraft.resources.Identifier
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult

