package me.muksc.tacztweaks.core;

/**
 * Scoped flag used while {@code EnchantmentHelper#getDamageProtection} is running
 * so {@code DamageSource#is(IS_PROJECTILE)} also matches {@code tacz:bullets}.
 * Kept as a ThreadLocal so endermen / other projectile checks are unaffected.
 */
public final class BulletProtection {
    private static final ThreadLocal<Boolean> TREAT_BULLETS_AS_PROJECTILE = ThreadLocal.withInitial(() -> false);

    private BulletProtection() {}

    public static void enter() {
        TREAT_BULLETS_AS_PROJECTILE.set(true);
    }

    public static void exit() {
        TREAT_BULLETS_AS_PROJECTILE.set(false);
    }

    public static boolean isActive() {
        return TREAT_BULLETS_AS_PROJECTILE.get();
    }
}
