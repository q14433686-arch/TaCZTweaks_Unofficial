package me.muksc.tacztweaks.mixin.tweaks;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import com.tacz.guns.sound.SoundManager;
import me.muksc.tacztweaks.TaCZTweaks;
import me.muksc.tacztweaks.config.Config;
import net.minecraft.world.entity.LivingEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Map;

/**
 * Two tweaks on the server-side gun script API:
 * <ul>
 *   <li>{@code forceFirstPersonShootingSound}: uses the first-person shoot sound (stereo)
 *       instead of the third-person one when other players shoot. The two {@code 3P_SOUND}
 *       static fields are read inside the named hook {@code runShootCycle} (the refabricated
 *       port renamed the upstream {@code lambda$shootOnce$2});</li>
 *   <li>{@code betterInaccuracy}: replaces the single-state inaccuracy lookup with the
 *       combined multi-state calculation in {@link TaCZTweaks#getBetterInaccuracy}.</li>
 * </ul>
 */
@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public abstract class ModernKineticGunScriptAPIMixin {
    @Shadow
    private LivingEntity shooter;

    // ---- forceFirstPersonShootingSound ----
    @ModifyExpressionValue(method = "runShootCycle", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC, target = "Lcom/tacz/guns/sound/SoundManager;SILENCE_3P_SOUND:Ljava/lang/String;"))
    private String tacztweaks$runShootCycle$forceFirstPersonShootSound$silenced(String original) {
        if (!Config.Tweaks.INSTANCE.forceFirstPersonShootingSound()) return original;
        return SoundManager.SILENCE_SOUND;
    }

    @ModifyExpressionValue(method = "runShootCycle", at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC, target = "Lcom/tacz/guns/sound/SoundManager;SHOOT_3P_SOUND:Ljava/lang/String;"))
    private String tacztweaks$runShootCycle$forceFirstPersonShootSound$normal(String original) {
        if (!Config.Tweaks.INSTANCE.forceFirstPersonShootingSound()) return original;
        return SoundManager.SHOOT_SOUND;
    }

    // ---- betterInaccuracy ----
    @WrapOperation(method = "shootOnce", at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"))
    private Object tacztweaks$shootOnce$betterInaccuracy(Map<InaccuracyType, Float> instance, Object key, Operation<Object> original) {
        if (!Config.Tweaks.INSTANCE.betterInaccuracy()) return original.call(instance, key);
        return TaCZTweaks.getBetterInaccuracy(instance, shooter);
    }
}
