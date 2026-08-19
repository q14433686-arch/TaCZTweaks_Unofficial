package me.muksc.tacztweaks.mixin.compat.lrtactical;

import me.muksc.tacztweaks.data.manager.MeleeInteractionManager;
import me.xjqsh.lrtactical.api.item.IMeleeWeapon;
import me.xjqsh.lrtactical.api.melee.MeleeAction;
import me.xjqsh.lrtactical.item.index.MeleeWeaponIndex;
import me.xjqsh.lrtactical.item.melee.CombatData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * LRTactical 1.21.11 routes a melee swing through IMeleeWeapon#performAttack.
 * If the swing hits no entities, try data-driven block interaction rules.
 */
@Mixin(value = IMeleeWeapon.class, remap = false)
public interface IMeleeWeaponMixin {
    @Inject(method = "performAttack", at = @At("RETURN"))
    default void tacztweaks$performAttack$handleBlock(
        ServerPlayer player,
        ItemStack stack,
        MeleeAction action,
        Vec3 origin,
        Vec3 direction,
        CallbackInfoReturnable<Integer> cir
    ) {
        if (cir.getReturnValueI() > 0) return;
        IMeleeWeapon self = (IMeleeWeapon) this;
        Optional<? extends MeleeWeaponIndex<?>> index = self.getMeleeIndex(stack);
        if (index.isEmpty()) return;
        CombatData.MeleeAttackInfo info = index.get().getData().getAttackInfo().getAttackInfo(action);
        if (info == null || info.getHitbox() == null) return;
        float damage = (float) (player.getAttributeValue(Attributes.ATTACK_DAMAGE) * info.getFactor());
        MeleeInteractionManager.INSTANCE.handleBlockInteraction(player, info.getHitbox().getMaxRange(), damage);
    }
}
