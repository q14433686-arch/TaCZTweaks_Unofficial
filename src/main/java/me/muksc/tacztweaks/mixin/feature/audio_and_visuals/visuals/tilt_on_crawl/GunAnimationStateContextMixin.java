package me.muksc.tacztweaks.mixin.feature.audio_and_visuals.visuals.tilt_on_crawl;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import me.muksc.tacztweaks.config.Config;
import me.muksc.tacztweaks.config.Config.AudioAndVisuals.Visuals.ETiltOnCrawl;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GunAnimationStateContext.class, remap = false)
public abstract class GunAnimationStateContextMixin {
    @Shadow private GunData gunData;

    /**
     * Operate on the stable public result instead of javac's lambda$shouldSlide$… method.
     * Re-checking canSlide preserves the original "pose && gun supports tilt" contract.
     */
    @ModifyReturnValue(method = "shouldSlide", at = @At("RETURN"))
    private boolean tacztweaks$shouldSlide$tiltOnCrawl(boolean original) {
        Entity entity = Minecraft.getInstance().getCameraEntity();
        if (entity == null || !entity.isVisuallyCrawling() || !gunData.canSlide()) return original;

        ETiltOnCrawl tiltOnCrawl = Config.AudioAndVisuals.Visuals.tiltOnCrawl();
        if (tiltOnCrawl == ETiltOnCrawl.NEVER) return false;
        if (tiltOnCrawl == ETiltOnCrawl.ALWAYS) return true;
        return original;
    }
}
