package me.muksc.tacztweaks.mixininterface.features;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Extra state and hooks injected into {@code EntityKineticBullet} for the data-driven
 * bullet interaction system.
 */
public interface EntityKineticBulletExtension {
    record DamageModifier(float flat, float multiplier) {}

    ItemStack tacztweaks$getGunStack();

    int tacztweaks$getBlockPierce();

    void tacztweaks$incrementBlockPierce();

    int tacztweaks$getEntityPierce();

    void tacztweaks$incrementEntityPierce();

    void tacztweaks$addDamageModifier(float flat, float multiplier);

    void tacztweaks$popDamageModifier();

    Vec3 tacztweaks$getPosition();

    void tacztweaks$setPosition(Vec3 position);
}
