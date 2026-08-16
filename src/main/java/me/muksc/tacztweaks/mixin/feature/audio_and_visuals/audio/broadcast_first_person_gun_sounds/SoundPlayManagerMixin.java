package me.muksc.tacztweaks.mixin.feature.audio_and_visuals.audio.broadcast_first_person_gun_sounds;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.sound.GunSoundInstance;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.config.common.GunConfig;
import com.tacz.guns.sound.SoundManager;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.network.NetworkManager;
import me.muksc.tacztweaks.network.message.ClientMessageBroadcastSound;
//~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(value = SoundPlayManager.class, remap = false)
public abstract class SoundPlayManagerMixin {
    @Unique
    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    private static void tacztweaks$broadcast(ResourceLocation name, float volume, float pitch, int distance) {
        if (Config.AudioAndVisuals.Audio.broadcastFirstPersonGunSounds() && name != null) {
            NetworkManager.sendC2S(new ClientMessageBroadcastSound(name, volume, pitch, distance));
        }
    }

    /** TaCZ resolves this sound in an Optional lambda; resolve it at the stable method boundary. */
    @Inject(method = "playerRefitSound", at = @At("HEAD"))
    private static void tacztweaks$playerRefitSound$broadcastFirstPersonGunSounds(
        ItemStack attachmentItem, LocalPlayer player, String soundName, CallbackInfo ci
    ) {
        IAttachment attachment = IAttachment.getIAttachmentOrNull(attachmentItem);
        if (attachment == null) return;
        TimelessAPI.getClientAttachmentIndex(attachment.getAttachmentId(attachmentItem)).ifPresent(index -> {
            //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
            Map<String, ResourceLocation> sounds = index.getSounds();
            tacztweaks$broadcast(sounds.get(soundName), 1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get());
        });
    }

    @Inject(method = "playReloadSound", at = @At("HEAD"))
    private static void tacztweaks$playReloadSound$broadcastFirstPersonGunSounds(
        LivingEntity entity, GunDisplayInstance display, boolean noAmmo, CallbackInfo ci
    ) {
        tacztweaks$broadcast(
            display.getSounds(noAmmo ? SoundManager.RELOAD_EMPTY_SOUND : SoundManager.RELOAD_TACTICAL_SOUND),
            1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get()
        );
    }

    @Inject(method = "playInspectSound", at = @At("HEAD"))
    private static void tacztweaks$playInspectSound$broadcastFirstPersonGunSounds(
        LivingEntity entity, GunDisplayInstance display, boolean noAmmo, CallbackInfo ci
    ) {
        tacztweaks$broadcast(
            display.getSounds(noAmmo ? SoundManager.INSPECT_EMPTY_SOUND : SoundManager.INSPECT_SOUND),
            1.0f, 1.0f, GunConfig.DEFAULT_GUN_OTHER_SOUND_DISTANCE.get()
        );
    }

    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    @WrapOperation(method = {
        "playDryFireSound",
        "playBoltSound",
        "playDrawSound",
        "playPutAwaySound",
        "playFireSelectSound",
        "playMeleeBayonetSound",
        "playMeleePushSound",
        "playMeleeStockSound"
    },
    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/sound/SoundPlayManager;playClientSound(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/resources/ResourceLocation;FFI)Lcom/tacz/guns/client/sound/GunSoundInstance;"))
    //~ if >=1.21.11 'ResourceLocation' -> 'Identifier'
    private static GunSoundInstance tacztweaks$playLocalSound$broadcastFirstPersonGunSounds(Entity entity, ResourceLocation name, float volume, float pitch, int distance, Operation<GunSoundInstance> original) {
        tacztweaks$broadcast(name, volume, pitch, distance);
        return original.call(entity, name, volume, pitch, distance);
    }
}
