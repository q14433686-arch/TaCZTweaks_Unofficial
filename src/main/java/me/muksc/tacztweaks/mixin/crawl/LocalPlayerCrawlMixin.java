package me.muksc.tacztweaks.mixin.crawl;

import com.tacz.guns.client.gameplay.LocalPlayerCrawl;
import me.muksc.tacztweaks.config.Config;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Completely disables TaCZ's crawl when {@code Config.Crawl.enabled} is false,
 * so that other mods' crawl implementations can work.
 */
@Mixin(value = LocalPlayerCrawl.class, remap = false)
public abstract class LocalPlayerCrawlMixin {
    @Inject(method = "crawl", at = @At("HEAD"), cancellable = true)
    private void tacztweaks$crawl$disableCrawl(boolean isCrawl, CallbackInfo ci) {
        if (!Config.Crawl.INSTANCE.enabled()) ci.cancel();
    }

    @Inject(method = "tickCrawl", at = @At("HEAD"), cancellable = true)
    private void tacztweaks$tickCrawl$disableTickCrawl(CallbackInfo ci) {
        if (!Config.Crawl.INSTANCE.enabled()) ci.cancel();
    }
}
