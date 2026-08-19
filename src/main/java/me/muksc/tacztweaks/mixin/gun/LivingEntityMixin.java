package me.muksc.tacztweaks.mixin.gun;

import me.muksc.tacztweaks.mixininterface.gun.SlideDataHolder;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin implements SlideDataHolder {
    @Unique
    private boolean tacztweaks$shouldSlide = false;

    @Unique
    private int tacztweaks$slideRequestExpiry;

    @Override
    public boolean tacztweaks$getShouldSlide() {
        return tacztweaks$shouldSlide;
    }

    @Override
    public void tacztweaks$setShouldSlide(boolean shouldSlide) {
        tacztweaks$shouldSlide = shouldSlide;
    }

    @Override
    public int tacztweaks$getSlideRequestExpiry() {
        return tacztweaks$slideRequestExpiry;
    }

    @Override
    public void tacztweaks$setSlideRequestExpiry(int tick) {
        tacztweaks$slideRequestExpiry = tick;
    }
}
