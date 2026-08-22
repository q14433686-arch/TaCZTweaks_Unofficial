package me.muksc.tacztweaks.client.config

import com.tacz.guns.resource.modifier.AttachmentPropertyManager
import dev.isxander.yacl3.api.*
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder
import dev.isxander.yacl3.dsl.*
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.config.Config
import me.muksc.tacztweaks.config.ConfigManager
import me.muksc.tacztweaks.config.sync.ESyncDirection
import me.muksc.tacztweaks.network.NetworkHandler
import me.muksc.tacztweaks.network.message.ClientMessageSyncConfig
import net.minecraft.ChatFormatting
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import java.text.DecimalFormat

/** Client-only YACL screen builder; common config persistence contains no Minecraft client types. */
object ConfigScreenFactory {
    @JvmStatic
    fun generateConfigScreen(parent: Screen?): Screen = with(Config) {
        YetAnotherConfigLib.createBuilder().apply {
        title(TaCZTweaks.translatable("config.title"))
        save {
            if (ConfigManager.syncedWithServer && Minecraft.getInstance().player?.let(ConfigManager::canUpdateServerConfig) == true) {
                NetworkHandler.sendC2S(ClientMessageSyncConfig.create())
            } else {
                Config.sync(ESyncDirection.NONE)
                run {
                    val server = Minecraft.getInstance().getSingleplayerServer() ?: return@run
                    val player = server.playerList.getPlayer(Minecraft.getInstance().player?.uuid ?: return@run) ?: return@run
                    AttachmentPropertyManager.postChangeEvent(player, player.mainHandItem)
                }
            }
            Config.runAsSaving(Config::saveToFile)
            Minecraft.getInstance().player?.also { player ->
                AttachmentPropertyManager.postChangeEvent(player, player.mainHandItem)
            }
        }

        val canUpdateServerConfig = when (ConfigManager.syncedWithServer) {
            true -> Minecraft.getInstance().player?.let(ConfigManager::canUpdateServerConfig) == true
            false -> true
        }

        fun <T> Option.Builder<T>.nameSynced(name: MutableComponent) {
            if (ConfigManager.syncedWithServer) name.withStyle(ChatFormatting.YELLOW)
            this.name(name)
        }

        fun <T> Option.Builder<T>.descriptionSynced(description: OptionDescription) {
            this.description(OptionDescription.createBuilder().apply {
                if (ConfigManager.syncedWithServer) {
                    text(TaCZTweaks.translatable("config.label.synced")
                        .append(Component.literal("\n"))
                        .withStyle(ChatFormatting.YELLOW))
                }
                text(description.text())
            }.build())
        }

        fun <T> ListOption.Builder<T>.nameSynced(name: MutableComponent) {
            if (ConfigManager.syncedWithServer) name.withStyle(ChatFormatting.YELLOW)
            this.name(name)
        }

        fun <T> ListOption.Builder<T>.descriptionSynced(description: OptionDescription) {
            this.description(OptionDescription.createBuilder().apply {
                if (ConfigManager.syncedWithServer) {
                    text(TaCZTweaks.translatable("config.label.synced")
                        .append(Component.literal("\n"))
                        .withStyle(ChatFormatting.YELLOW))
                }
                text(description.text())
            }.build())
        }

        category(ConfigCategory.createBuilder().apply {
            name(TaCZTweaks.translatable("config.category.general"))
            group(OptionGroup.createBuilder().apply {
                name(TaCZTweaks.translatable("config.gun"))
                option(Option.createBuilder<Double>().apply {
                    name(TaCZTweaks.translatable("config.gun.reduceSensitivityKeyMultiplier.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.gun.reduceSensitivityKeyMultiplier.description")))
                    binding(Gun.reduceSensitivityKeyMultiplier.asBinding())
                    controller(slider(range = 0.0..1.0, step = 0.01) {
                        TaCZTweaks.translatable("config.label.multiplier", "%.2f".format(it))
                    })
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.gun.disableReduceSensitivityKeyWhileAiming.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.gun.disableReduceSensitivityKeyWhileAiming.description")))
                    binding(Gun.disableReduceSensitivityKeyWhileAiming.asBinding())
                    controller(booleanController())
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.gun.tiltGunKeyTriggersReduceSensitivity.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.gun.tiltGunKeyTriggersReduceSensitivity.description")))
                    binding(Gun.tiltGunKeyTriggersReduceSensitivity.asBinding())
                    controller(booleanController())
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.gun.tiltGunKeyCancelsSprint.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.gun.tiltGunKeyCancelsSprint.description")))
                    binding(Gun.tiltGunKeyCancelsSprint.asBinding())
                    controller(booleanController())
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.gun.cancelInspection.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.gun.cancelInspection.description")))
                    binding(Gun.cancelInspection.asBinding())
                    controller(booleanController())
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.gun.disableBulletCulling.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.gun.disableBulletCulling.description")))
                    binding(Gun.disableBulletCulling.asBinding())
                    controller(booleanController())
                }.build())
            }.build())
            group(OptionGroup.createBuilder().apply {
                name(TaCZTweaks.translatable("config.crawl"))
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.crawl.visualTweak.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.crawl.visualTweak.description")))
                    binding(Crawl.visualTweak.asBinding())
                    controller(booleanController())
                }.build())
                option(Option.createBuilder<Crawl.ETiltGun>().apply {
                    name(TaCZTweaks.translatable("config.crawl.tiltGun.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.crawl.tiltGun.description")))
                    binding(Crawl.tiltGun.asBinding())
                    controller(enumSwitch {
                        TaCZTweaks.translatable("config.label.ETiltGun.${it.name}")
                    })
                }.build())
            }.build())
            group(OptionGroup.createBuilder().apply {
                name(TaCZTweaks.translatable("config.compat"))
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.compat.firstAidCompat.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.compat.firstAidCompat.description")))
                    binding(Compat.firstAidCompat.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
            }.build())
            group(OptionGroup.createBuilder().apply {
                name(TaCZTweaks.translatable("config.tweaks"))
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.tweaks.alwaysFilterByHand.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.alwaysFilterByHand.description")))
                    binding(Tweaks.alwaysFilterByHand.asBinding())
                    controller(booleanController())
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.tweaks.rps.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.rps.description")))
                    binding(Tweaks.rps.asBinding())
                    controller(booleanController())
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.tweaks.forceDefaultHitAndKillSounds.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.forceDefaultHitAndKillSounds.description")))
                    binding(Tweaks.forceDefaultHitAndKillSounds.asBinding())
                    controller(booleanController())
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.tweaks.suppressHeadHitSounds.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.suppressHeadHitSounds.description")))
                    binding(Tweaks.suppressHeadHitSounds.asBinding())
                    controller(booleanController())
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.tweaks.suppressFleshHitSounds.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.suppressFleshHitSounds.description")))
                    binding(Tweaks.suppressFleshHitSounds.asBinding())
                    controller(booleanController())
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.tweaks.suppressKillSounds.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.suppressKillSounds.description")))
                    binding(Tweaks.suppressKillSounds.asBinding())
                    controller(booleanController())
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.tweaks.hideHitMarkers.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.hideHitMarkers.description")))
                    binding(Tweaks.hideHitMarkers.asBinding())
                    controller(booleanController())
                }.build())
            }.build())
            group(OptionGroup.createBuilder().apply {
                name(TaCZTweaks.translatable("config.debug"))
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.debug.bulletInteractions.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.debug.bulletInteractions.description")))
                    binding(Debug.bulletInteractions.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.debug.bulletParticles.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.debug.bulletParticles.description")))
                    binding(Debug.bulletParticles.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.debug.bulletSounds.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.debug.bulletSounds.description")))
                    binding(Debug.bulletSounds.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.debug.meleeInteractions.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.debug.meleeInteractions.description")))
                    binding(Debug.meleeInteractions.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
            }.build())
        }.build())
        category(ConfigCategory.createBuilder().apply {
            name(TaCZTweaks.translatable("config.category.gameplay"))
            group(OptionGroup.createBuilder().apply {
                name(TaCZTweaks.translatable("config.gun"))
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.gun.shootWhileSprinting.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.gun.shootWhileSprinting.description")))
                    binding(Gun.shootWhileSprinting.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.gun.sprintWhileReloading.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.gun.sprintWhileReloading.description")))
                    binding(Gun.sprintWhileReloading.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.gun.reloadWhileShooting.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.gun.reloadWhileShooting.description")))
                    binding(Gun.reloadWhileShooting.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.gun.reloadDiscardsMagazine.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.gun.reloadDiscardsMagazine.description")))
                    binding(Gun.reloadDiscardsMagazine.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.gun.fireSelectWhileShooting.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.gun.fireSelectWhileShooting.description")))
                    binding(Gun.fireSelectWhileShooting.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.gun.disableUnderwater.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.gun.disableUnderwater.description")))
                    binding(Gun.disableUnderwater.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.gun.manualBolting.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.gun.manualBolting.description")))
                    binding(Gun.manualBolting.asBinding())
                    controller(booleanController())
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.gun.allowUnload.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.gun.allowUnload.description")))
                    binding(Gun.allowUnload.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.gun.unloadBulletInBarrel.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.gun.unloadBulletInBarrel.description")))
                    binding(Gun.unloadBulletInBarrel.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
            }.build())
            group(ListOption.createBuilder<String>().apply {
                nameSynced(TaCZTweaks.translatable("config.gun.reloadDiscardsMagazineExclusions.name"))
                descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.gun.reloadDiscardsMagazineExclusions.description")))
                collapsed(true)
                binding(Gun.reloadDiscardsMagazineExclusions.asSyncedBinding())
                controller(stringField())
                initial("")
                available(canUpdateServerConfig)
            }.build())
            group(OptionGroup.createBuilder().apply {
                name(TaCZTweaks.translatable("config.crawl"))
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.crawl.enabled.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.crawl.enabled.description")))
                    binding(Crawl.enabled.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Float>().apply {
                    name(TaCZTweaks.translatable("config.crawl.pitchUpperLimit.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.crawl.pitchUpperLimit.description")))
                    binding(Crawl.pitchUpperLimit.asBinding())
                    controller(slider(range = 0.0F..90.0F, step = 1.0F) {
                        TaCZTweaks.translatable("config.label.degree", "%.1f".format(it))
                    })
                }.build())
                option(Option.createBuilder<Float>().apply {
                    name(TaCZTweaks.translatable("config.crawl.pitchLowerLimit.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.crawl.pitchLowerLimit.description")))
                    binding(Crawl.pitchLowerLimit.asBinding())
                    controller(slider(range = -90.0F..0.0F, step = 1.0F) {
                        TaCZTweaks.translatable("config.label.degree", "%.1f".format(it))
                    })
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.crawl.dynamicPitchLimit.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.crawl.dynamicPitchLimit.description")))
                    binding(Crawl.dynamicPitchLimit.asBinding())
                    controller(booleanController())
                }.build())
            }.build())
            group(OptionGroup.createBuilder().apply {
                name(TaCZTweaks.translatable("config.tweaks"))
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.tweaks.audibleFirstPersonGunSounds.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.audibleFirstPersonGunSounds.description")))
                    binding(Tweaks.audibleFirstPersonGunSounds.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.tweaks.forceFirstPersonShootingSound.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.forceFirstPersonShootingSound.description")))
                    binding(Tweaks.forceFirstPersonShootingSound.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.tweaks.betterInaccuracy.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.betterInaccuracy.description")))
                    binding(Tweaks.betterInaccuracy.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    name(TaCZTweaks.translatable("config.tweaks.betterMonoConversion.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.betterMonoConversion.description")))
                    binding(Tweaks.betterMonoConversion.asBinding())
                    controller(booleanController())
                    flag(OptionFlag.ASSET_RELOAD)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.tweaks.bulletProtection.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.bulletProtection.description")))
                    binding(Tweaks.bulletProtection.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.tweaks.betterGunTilt.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.betterGunTilt.description")))
                    binding(Tweaks.betterGunTilt.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.tweaks.endermenEvadeBullets.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.endermenEvadeBullets.description")))
                    binding(Tweaks.endermenEvadeBullets.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.tweaks.disableRefitOnAdventure.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.disableRefitOnAdventure.description")))
                    binding(Tweaks.disableRefitOnAdventure.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
                option(Option.createBuilder<Boolean>().apply {
                    nameSynced(TaCZTweaks.translatable("config.tweaks.infiniteAmmoDisablesConsumption.name"))
                    descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.tweaks.infiniteAmmoDisablesConsumption.description")))
                    binding(Tweaks.infiniteAmmoDisablesConsumption.asSyncedBinding())
                    controller(booleanController())
                    available(canUpdateServerConfig)
                }.build())
            }.build())
        }.build())
        category(ConfigCategory.createBuilder().apply {
            name(TaCZTweaks.translatable("config.category.balancing"))
            for ((key, modifier) in listOf(
                "damage" to Modifiers.Damage,
                "playerDamage" to Modifiers.PlayerDamage,
                "headshot" to Modifiers.Headshot,
                "playerHeadshot" to Modifiers.PlayerHeadshot,
                "armorIgnore" to Modifiers.ArmorIgnore,
                "speed" to Modifiers.Speed,
                "gravity" to Modifiers.Gravity,
                "friction" to Modifiers.Friction,
                "aimTime" to Modifiers.AimTime,
                "inaccuracy" to Modifiers.Inaccuracy,
                "standInaccuracy" to Modifiers.StandInaccuracy,
                "aimInaccuracy" to Modifiers.AimInaccuracy,
                "moveInaccuracy" to Modifiers.MoveInaccuracy,
                "sneakInaccuracy" to Modifiers.SneakInaccuracy,
                "crawlInaccuracy" to Modifiers.CrawlInaccuracy,
                "rpm" to Modifiers.RPM,
                "verticalRecoil" to Modifiers.VerticalRecoil,
                "horizontalRecoil" to Modifiers.HorizontalRecoil,
                "aimVerticalRecoil" to Modifiers.AimVerticalRecoil,
                "aimHorizontalRecoil" to Modifiers.AimHorizontalRecoil,
                "crawlVerticalRecoil" to Modifiers.CrawlVerticalRecoil,
                "crawlHorizontalRecoil" to Modifiers.CrawlHorizontalRecoil
            )) {
                group(OptionGroup.createBuilder().apply {
                    name(TaCZTweaks.translatable("config.modifiers.$key.name"))
                    description(OptionDescription.of(TaCZTweaks.translatable("config.modifiers.$key.description")))
                    collapsed(true)
                    option(Option.createBuilder<Double>().apply {
                        nameSynced(TaCZTweaks.translatable("config.modifier.addend.name"))
                        descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.modifier.addend.description")))
                        binding(modifier.addend.asSyncedBinding())
                        controller(numberField { value: Double ->
                            Component.literal(DecimalFormat("+#.#;-#.#").format(value))
                        })
                        available(canUpdateServerConfig)
                    }.build())
                    option(Option.createBuilder<Double>().apply {
                        nameSynced(TaCZTweaks.translatable("config.modifier.multiplier.name"))
                        descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.modifier.multiplier.description")))
                        binding(modifier.multiplier.asSyncedBinding())
                        controller(numberField { value: Double ->
                            Component.literal("%.1f".format(value))
                        })
                        available(canUpdateServerConfig)
                    }.build())
                    option(Option.createBuilder<String>().apply {
                        nameSynced(TaCZTweaks.translatable("config.modifier.function.name"))
                        descriptionSynced(OptionDescription.of(TaCZTweaks.translatable("config.modifier.function.description")))
                        binding(modifier.function.asSyncedBinding())
                        controller(stringField())
                        available(canUpdateServerConfig)
                    }.build())
                }.build())
            }
        }.build())
        }.build().generateScreen(parent)
    }

    private fun booleanController(): ControllerBuilderFactory<Boolean> = { option ->
        BooleanControllerBuilder.create(option)
            .formatValue { when (it) {
                true -> TaCZTweaks.translatable("config.label.enabled")
                false -> TaCZTweaks.translatable("config.label.disabled")
            } }
            .coloured(true)
    }

}
