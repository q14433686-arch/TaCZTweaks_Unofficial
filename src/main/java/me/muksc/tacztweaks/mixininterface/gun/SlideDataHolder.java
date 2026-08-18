package me.muksc.tacztweaks.mixininterface.gun;

public interface SlideDataHolder {
    boolean tacztweaks$getShouldSlide();

    void tacztweaks$setShouldSlide(boolean shouldSlide);

    int tacztweaks$getSlideRequestExpiry();

    void tacztweaks$setSlideRequestExpiry(int tick);
}
