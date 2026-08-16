package me.muksc.tacztweaks.mixin.feature.general.fixes.crawl_cooldown_fix;

import com.tacz.guns.client.gameplay.LocalPlayerCrawl;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LocalPlayerCrawl.class, remap = false)
public abstract class LocalPlayerCrawlMixin {
    @Shadow private int crawCooldownTicks;
    @Unique private int tacztweaks$crawlCooldownBeforeCall;

    /** Temporarily bypass the cooldown only for uncrawling, then restore its remaining value. */
    @Inject(method = "crawl", at = @At("HEAD"))
    private void tacztweaks$crawl$prepareCooldown(boolean isCrawl, CallbackInfo ci) {
        tacztweaks$crawlCooldownBeforeCall = crawCooldownTicks;
        if (Config.General.Fixes.crawlCooldownFix() && !isCrawl) {
            crawCooldownTicks = 0;
        }
    }

    @Inject(method = "crawl", at = @At("RETURN"))
    private void tacztweaks$crawl$restoreCooldownWhenUncrawling(boolean isCrawl, CallbackInfo ci) {
        if (Config.General.Fixes.crawlCooldownFix() && !isCrawl) {
            crawCooldownTicks = tacztweaks$crawlCooldownBeforeCall;
        }
    }
}
