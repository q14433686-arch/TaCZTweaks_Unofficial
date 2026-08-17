package me.muksc.tacztweaks.mixin.crawl;

import com.tacz.guns.entity.shooter.LivingEntityCrawl;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Server-side counterpart of {@link LocalPlayerCrawlMixin}: disables TaCZ's crawl
 * entirely when {@code Config.Crawl.enabled} is false.
 */
@Mixin(value = LivingEntityCrawl.class, remap = false)
public abstract class LivingEntityCrawlMixin {
    @Inject(method = "crawl", at = @At("HEAD"), cancellable = true)
    private void tacztweaks$crawl$disableCrawl(boolean isCrawl, CallbackInfo ci) {
        if (!Config.Crawl.INSTANCE.enabled()) ci.cancel();
    }

    @Inject(method = "tickCrawling", at = @At("HEAD"), cancellable = true)
    private void tacztweaks$tickCrawling$disableTickCrawling(CallbackInfo ci) {
        if (!Config.Crawl.INSTANCE.enabled()) ci.cancel();
    }
}
