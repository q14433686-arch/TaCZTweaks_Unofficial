package me.muksc.tacztweaks.mixin.feature.general.compatibility.lrtactical;

//? if fabric && >=1.21.11 {
/*import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.muksc.tacztweaks.feature.datapack.legacy.manager.MeleeInteractionManager;
import me.xjqsh.lrtactical.api.item.IMeleeWeapon;
import me.xjqsh.lrtactical.api.melee.MeleeAction;
import me.xjqsh.lrtactical.item.melee.CombatData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = IMeleeWeapon.class, remap = false)
public abstract class BuiltinMeleeWeaponMixin {
    @ModifyReturnValue(method = "performAttack", at = @At("RETURN"))
    private int tacztweaks$performAttack$handleBlockInteraction(
        int hitCount,
        ServerPlayer attacker,
        ItemStack stack,
        MeleeAction action,
        Vec3 origin,
        Vec3 direction
    ) {
        if (hitCount != 0) return hitCount;
        IMeleeWeapon weapon = IMeleeWeapon.class.cast(this);
        var index = weapon.getMeleeIndex(stack).orElse(null);
        if (index == null) return hitCount;
        CombatData.MeleeAttackInfo attackInfo = index.getData().getAttackInfo().getAttackInfo(action);
        if (attackInfo == null) return hitCount;

        float damage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE) * attackInfo.getFactor();
        MeleeInteractionManager.handleBlockInteraction(
            attacker,
            attackInfo.getHitbox().getMaxRange(),
            damage
        );
        return hitCount;
    }
}
*///?} else {
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Minecraft.class)
public abstract class BuiltinMeleeWeaponMixin { }
//?}
