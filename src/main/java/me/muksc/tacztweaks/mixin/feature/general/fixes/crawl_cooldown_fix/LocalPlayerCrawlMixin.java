package me.muksc.tacztweaks.mixin.feature.general.fixes.crawl_cooldown_fix;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
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

    @Definition(id = "crawCooldownTicks", field = "Lcom/tacz/guns/client/gameplay/LocalPlayerCrawl;crawCooldownTicks:I")
    @Expression("this.crawCooldownTicks > 0")
    @ModifyExpressionValue(method = "crawl", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean tacztweaks$crawl$crawlCooldownFix(
        boolean original,
        @Local(argsOnly = true) boolean isCrawl
    ) {
        if (!Config.General.Fixes.crawlCooldownFix()) return original;
        return original && isCrawl;
    }

    @Inject(method = "crawl", at = @At("HEAD"))
    private void tacztweaks$crawl$rememberCooldown(boolean isCrawl, CallbackInfo ci) {
        tacztweaks$crawlCooldownBeforeCall = crawCooldownTicks;
    }

    /**
     * Uncrawling must not restart the cooldown. Restoring the entry value at RETURN has the
     * same result as suppressing the assignment inside TaCZ's Optional lambda, without naming it.
     */
    @Inject(method = "crawl", at = @At("RETURN"))
    private void tacztweaks$crawl$restoreCooldownWhenUncrawling(boolean isCrawl, CallbackInfo ci) {
        if (Config.General.Fixes.crawlCooldownFix() && !isCrawl) {
            crawCooldownTicks = tacztweaks$crawlCooldownBeforeCall;
        }
    }
}
