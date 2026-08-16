package me.muksc.tacztweaks.mixin.feature.datapack.bullet_sounds;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.TacHitResult;
import me.muksc.tacztweaks.feature.datapack.legacy.manager.BulletSoundsManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector2d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class EntityKineticBulletMixin  {
    @Unique
    private final List<ServerPlayer> tacztweaks$hitPlayers = new ArrayList<>();

    @Inject(method = "shootFromRotation", at = @At("TAIL"))
    private void tacztweaks$shootFromRotation$handleAirspace(
        Entity shooter,
        float pitch,
        float yaw,
        float roll,
        float velocity,
        Vector2d spread,
        CallbackInfo ci
    ) {
        BulletSoundsManager.INSTANCE.handleAirspace(EntityKineticBullet.class.cast(this));
    }

    @Inject(method = "onHitEntity", at = @At("HEAD"))
    private void tacztweaks$onHitEntity$onHitPlayer(TacHitResult result, Vec3 startVec, Vec3 endVec, CallbackInfo ci) {
        if (!(result.getEntity() instanceof ServerPlayer player)) return;
        tacztweaks$hitPlayers.add(player);
    }

    @Inject(method = "onBulletTick", at = @At("RETURN"))
    private void tacztweaks$onBulletTick$tick(CallbackInfo ci) {
        if (EntityKineticBullet.class.cast(this).isRemoved()) return;
        EntityKineticBullet instance = EntityKineticBullet.class.cast(this);
        BulletSoundsManager.INSTANCE.handleConstant(instance);
        BulletSoundsManager.INSTANCE.handleSoundWhizz(instance, tacztweaks$hitPlayers);
    }
}