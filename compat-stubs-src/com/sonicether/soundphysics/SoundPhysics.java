package com.sonicether.soundphysics;

import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

/** Compile-time stub for optional Sound Physics compatibility targets. */
public final class SoundPhysics {
    private SoundPhysics() {
    }

    public static double calculateOcclusion(Vec3 a, Vec3 b, SoundSource source, Identifier sound) {
        return 0.0D;
    }

    public static void setEnvironment(int sourceId, float a, float b, float c, float d, float e, float f, float g, float h, float i, float j) {
    }

    public static Vec3 evaluateEnvironment(int sourceId, double x, double y, double z, SoundSource category, Identifier sound, boolean auxOnly) {
        int shared = new ReflectedAudio().getSharedAirspaces();
        double occlusion = calculateOcclusion(Vec3.ZERO, Vec3.ZERO, category, sound);
        float[] bounceReflectivityRatio = new float[] {0.0F};
        if (bounceReflectivityRatio.length > 0) {
            setEnvironment(sourceId, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, (float) occlusion, shared);
        }
        return new Vec3(x, y, z);
    }
}
