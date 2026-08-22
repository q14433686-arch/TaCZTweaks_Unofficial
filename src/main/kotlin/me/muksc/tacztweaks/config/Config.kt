package me.muksc.tacztweaks.config

import com.google.common.collect.Lists
import com.mojang.serialization.Codec
import com.tacz.guns.resource.modifier.AttachmentPropertyManager
import com.tacz.guns.resource.pojo.data.attachment.Modifier
import dev.isxander.yacl3.config.v3.register
import dev.isxander.yacl3.config.v3.value
import dev.isxander.yacl3.platform.YACLPlatform
import me.muksc.tacztweaks.TaCZTweaks
import me.muksc.tacztweaks.config.sync.SyncableCodecConfig
import me.muksc.tacztweaks.config.sync.SyncableJsonFileCodecConfig
import me.muksc.tacztweaks.mixin.accessor.ModifierAccessor
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.util.StringRepresentable

@Suppress("UnstableApiUsage")
object Config : SyncableJsonFileCodecConfig<Config>(
    YACLPlatform.getConfigDir().resolve("${TaCZTweaks.MOD_ID}.json")
) {
    init {
        registerSyncable("gun", Gun)
        registerSyncable("modifiers", Modifiers)
        registerSyncable("crawl", Crawl)
        registerSyncable("compat", Compat)
        registerSyncable("tweaks", Tweaks)
        registerSyncable("debug", Debug)
    }

    object Gun : SyncableCodecConfig<Gun>() {
        val shootWhileSprinting by registerSyncable(
            default = true,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val sprintWhileReloading by registerSyncable(
            default = true,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val reloadWhileShooting by registerSyncable(
            default = true,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val reloadDiscardsMagazine by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val reloadDiscardsMagazineExclusions by registerSyncable(
            default = listOf("tacz:m870", "tacz:db_short", "tacz:db_long"),
            codec = Codec.list(STRING),
            encoder = { buf, value -> buf.writeCollection(value, FriendlyByteBuf::writeUtf) },
            decoder = { buf -> buf.readCollection(Lists::newArrayListWithCapacity, FriendlyByteBuf::readUtf) }
        )
        val fireSelectWhileShooting by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val disableUnderwater by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val manualBolting by register(false, BOOL)
        val allowUnload by registerSyncable(
            default = true,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val unloadBulletInBarrel by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val reduceSensitivityKeyMultiplier by register(0.5, DOUBLE)
        val disableReduceSensitivityKeyWhileAiming by register(false, BOOL)
        val tiltGunKeyCancelsSprint by register(true, BOOL)
        val tiltGunKeyTriggersReduceSensitivity by register(true, BOOL)
        val cancelInspection by register(false, BOOL)
        val disableBulletCulling by register(false, BOOL)

        fun shootWhileSprinting(): Boolean = shootWhileSprinting.syncedValue
        fun sprintWhileReloading(): Boolean = sprintWhileReloading.syncedValue
        fun reloadWhileShooting(): Boolean = reloadWhileShooting.syncedValue
        fun reloadDiscardsMagazine(): Boolean = reloadDiscardsMagazine.syncedValue
        fun reloadDiscardsMagazineExclusions(): List<String> = reloadDiscardsMagazineExclusions.syncedValue
        fun fireSelectWhileShooting(): Boolean = fireSelectWhileShooting.syncedValue
        fun disableUnderwater(): Boolean = disableUnderwater.syncedValue
        fun manualBolting(): Boolean = manualBolting.value
        fun allowUnload(): Boolean = allowUnload.syncedValue
        fun unloadBulletInBarrel(): Boolean = unloadBulletInBarrel.syncedValue
        fun reduceSensitivityKeyMultiplier(): Double = reduceSensitivityKeyMultiplier.value
        fun disableReduceSensitivityKeyWhileAiming(): Boolean = disableReduceSensitivityKeyWhileAiming.value
        fun tiltGunKeyCancelsSprint(): Boolean = tiltGunKeyCancelsSprint.value
        fun tiltGunKeyTriggersReduceSensitivity(): Boolean = tiltGunKeyTriggersReduceSensitivity.value
        fun cancelInspection(): Boolean = cancelInspection.value
        fun disableBulletCulling(): Boolean = disableBulletCulling.value
    }

    abstract class ModifierConfig : SyncableCodecConfig<ModifierConfig>() {
        val addend by registerSyncable(
            default = 0.0,
            codec = DOUBLE,
            encoder = FriendlyByteBuf::writeDouble,
            decoder = FriendlyByteBuf::readDouble
        )
        val multiplier by registerSyncable(
            default = 1.0,
            codec = DOUBLE,
            encoder = FriendlyByteBuf::writeDouble,
            decoder = FriendlyByteBuf::readDouble
        )
        val function by registerSyncable<String>(
            default = "",
            codec = STRING,
            encoder = FriendlyByteBuf::writeUtf,
            decoder = FriendlyByteBuf::readUtf
        )

        fun isEmpty(): Boolean =
            addend.syncedValue == 0.0
                && multiplier.syncedValue == 1.0
                && function.syncedValue.isEmpty()

        fun eval(value: Double): Double {
            if (isEmpty()) return value
            return AttachmentPropertyManager.eval(modifier, value)
        }

        private val _modifier = Modifier()
        private val accessor = _modifier as ModifierAccessor
        val modifier: Modifier get() = _modifier.apply {
            accessor.setAddend(this@ModifierConfig.addend.syncedValue)
            accessor.setMultiplier(this@ModifierConfig.multiplier.syncedValue)
            accessor.setFunction(this@ModifierConfig.function.syncedValue)
        }
    }

    object Modifiers : SyncableCodecConfig<Modifiers>() {
        init {
            registerSyncable("damage", Damage)
            registerSyncable("playerDamage", PlayerDamage)
            registerSyncable("headshot", Headshot)
            registerSyncable("playerHeadshot", PlayerHeadshot)
            registerSyncable("armorIgnore", ArmorIgnore)
            registerSyncable("speed", Speed)
            registerSyncable("gravity", Gravity)
            registerSyncable("friction", Friction)
            registerSyncable("aimTime", AimTime)
            registerSyncable("inaccuracy", Inaccuracy)
            registerSyncable("standInaccuracy", StandInaccuracy)
            registerSyncable("aimInaccuracy", AimInaccuracy)
            registerSyncable("moveInaccuracy", MoveInaccuracy)
            registerSyncable("sneakInaccuracy", SneakInaccuracy)
            registerSyncable("crawlInaccuracy", CrawlInaccuracy)
            registerSyncable("rpm", RPM)
            registerSyncable("verticalRecoil", VerticalRecoil)
            registerSyncable("horizontalRecoil", HorizontalRecoil)
            registerSyncable("aimVerticalRecoil", AimVerticalRecoil)
            registerSyncable("aimHorizontalRecoil", AimHorizontalRecoil)
            registerSyncable("crawlVerticalRecoil", CrawlVerticalRecoil)
            registerSyncable("crawlHorizontalRecoil", CrawlHorizontalRecoil)
        }

        object Damage : ModifierConfig()
        object PlayerDamage : ModifierConfig()
        object Headshot : ModifierConfig()
        object PlayerHeadshot : ModifierConfig()
        object ArmorIgnore : ModifierConfig()
        object Speed : ModifierConfig()
        object Gravity : ModifierConfig()
        object Friction : ModifierConfig()
        object AimTime : ModifierConfig()
        object Inaccuracy : ModifierConfig()
        object StandInaccuracy : ModifierConfig()
        object AimInaccuracy : ModifierConfig()
        object MoveInaccuracy : ModifierConfig()
        object SneakInaccuracy : ModifierConfig()
        object CrawlInaccuracy : ModifierConfig()
        object RPM : ModifierConfig()
        object VerticalRecoil : ModifierConfig()
        object HorizontalRecoil : ModifierConfig()
        object AimVerticalRecoil : ModifierConfig()
        object AimHorizontalRecoil : ModifierConfig()
        object CrawlVerticalRecoil : ModifierConfig()
        object CrawlHorizontalRecoil : ModifierConfig()
    }

    object Crawl : SyncableCodecConfig<Crawl>() {
        val enabled by registerSyncable(
            default = true,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val pitchUpperLimit by register(25.0F, FLOAT)
        val pitchLowerLimit by register(-10.0F, FLOAT)
        val dynamicPitchLimit by register(false, BOOL)
        val visualTweak by register(true, BOOL)
        val tiltGun by register(ETiltGun.DEFAULT, ETiltGun.CODEC)

        enum class ETiltGun : StringRepresentable {
            DEFAULT,
            ALWAYS,
            NEVER;

            override fun getSerializedName(): String = name

            companion object {
                val CODEC: Codec<ETiltGun> = StringRepresentable.fromEnum(::values)
            }
        }

        fun enabled(): Boolean = enabled.syncedValue
        fun pitchUpperLimit(): Float = pitchUpperLimit.value
        fun pitchLowerLimit(): Float = pitchLowerLimit.value
        fun dynamicPitchLimit(): Boolean = dynamicPitchLimit.value
        fun visualTweak(): Boolean = visualTweak.value
        fun tiltGun(): ETiltGun = tiltGun.value
    }

    object Compat : SyncableCodecConfig<Compat>() {
        val firstAidCompat by registerSyncable(
            default = true,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )

        fun firstAidCompat(): Boolean = firstAidCompat.syncedValue
    }

    object Tweaks : SyncableCodecConfig<Tweaks>() {
        val audibleFirstPersonGunSounds by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val forceFirstPersonShootingSound by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val betterMonoConversion by register(false, BOOL)
        val betterInaccuracy by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val betterGunTilt by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val bulletProtection by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val endermenEvadeBullets by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val disableRefitOnAdventure by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val infiniteAmmoDisablesConsumption by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val alwaysFilterByHand by register(true, BOOL)
        val rps by register(false, BOOL)
        val forceDefaultHitAndKillSounds by register(false, BOOL)
        val suppressHeadHitSounds by register(false, BOOL)
        val suppressFleshHitSounds by register(false, BOOL)
        val suppressKillSounds by register(false, BOOL)
        val hideHitMarkers by register(false, BOOL)

        fun audibleFirstPersonGunSounds(): Boolean = audibleFirstPersonGunSounds.syncedValue
        fun forceFirstPersonShootingSound(): Boolean = forceFirstPersonShootingSound.syncedValue
        fun betterMonoConversion(): Boolean = betterMonoConversion.value
        fun betterInaccuracy(): Boolean = betterInaccuracy.syncedValue
        fun betterGunTilt(): Boolean = betterGunTilt.syncedValue
        fun bulletProtection(): Boolean = bulletProtection.syncedValue
        fun endermenEvadeBullets(): Boolean = endermenEvadeBullets.syncedValue
        fun disableRefitOnAdventure(): Boolean = disableRefitOnAdventure.syncedValue
        fun infiniteAmmoDisablesConsumption(): Boolean = infiniteAmmoDisablesConsumption.syncedValue
        fun alwaysFilterByHand(): Boolean = alwaysFilterByHand.value
        fun rps(): Boolean = rps.value
        fun forceDefaultHitAndKillSounds(): Boolean = forceDefaultHitAndKillSounds.value
        fun suppressHeadHitSounds(): Boolean = suppressHeadHitSounds.value
        fun suppressFleshHitSounds(): Boolean = suppressFleshHitSounds.value
        fun suppressKillSounds(): Boolean = suppressKillSounds.value
        fun hideHitMarkers(): Boolean = hideHitMarkers.value
    }

    object Debug : SyncableCodecConfig<Debug>() {
        val bulletInteractions by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val bulletParticles by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val bulletSounds by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )
        val meleeInteractions by registerSyncable(
            default = false,
            codec = BOOL,
            encoder = FriendlyByteBuf::writeBoolean,
            decoder = FriendlyByteBuf::readBoolean
        )

        fun bulletInteractions(): Boolean = bulletInteractions.syncedValue
        fun bulletParticles(): Boolean = bulletParticles.syncedValue
        fun bulletSounds(): Boolean = bulletSounds.syncedValue
        fun meleeInteractions(): Boolean = meleeInteractions.syncedValue
    }

    fun initialize() {
        check(syncableEntries.size == 6) { "TaCZ Tweaks config groups were not fully registered" }
    }

    init {
        loadFromFile()
        saveToFile()
    }
}