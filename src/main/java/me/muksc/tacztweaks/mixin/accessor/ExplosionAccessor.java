package me.muksc.tacztweaks.mixin.accessor;

//? if <1.21.11 {
import net.minecraft.world.level.Explosion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Explosion.class)
public interface ExplosionAccessor {
    @Accessor("x")
    void tacztweaks$setX(double x);

    @Accessor("y")
    void tacztweaks$setY(double y);

    @Accessor("z")
    void tacztweaks$setZ(double z);
}
//?} else {
/*import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;

// Explosion became an interface; the only consumer is the 1.20.1 VS compatibility path.
@Mixin(Minecraft.class)
public interface ExplosionAccessor { }
*///?}
