package me.muksc.tacztweaks.mixin.features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.TacHitResult;
import me.muksc.tacztweaks.data.manager.BulletInteractionManager;
import me.muksc.tacztweaks.data.manager.BulletParticlesManager;
import me.muksc.tacztweaks.data.manager.BulletSoundsManager;
import me.muksc.tacztweaks.mixininterface.features.EntityKineticBulletExtension;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class EntityKineticBulletMixin implements EntityKineticBulletExtension {
    @Unique
    private ItemStack tacztweaks$gunStack = null;

    @Unique
    private int tacztweaks$blockPierce = 0;

    @Unique
    private int tacztweaks$entityPierce = 0;

    @Unique
    private final List<DamageModifier> tacztweaks$damageModifiers = new LinkedList<>();

    @Unique
    private Vec3 tacztweaks$position = Vec3.ZERO;

    @Unique
    private final List<ServerPlayer> tacztweaks$hitPlayers = new ArrayList<>();

    @Unique
    private final Set<UUID> tacztweaks$whizzedPlayers = new HashSet<>();

    @Unique
    private int tacztweaks$burstIndex = 0;

    @Unique
    private int tacztweaks$pelletIndex = 0;

    @Override
    public ItemStack tacztweaks$getGunStack() {
        return tacztweaks$gunStack;
    }

    @Override
    public int tacztweaks$getBlockPierce() {
        return tacztweaks$blockPierce;
    }

    @Override
    public void tacztweaks$incrementBlockPierce() {
        tacztweaks$blockPierce++;
    }

    @Override
    public int tacztweaks$getEntityPierce() {
        return tacztweaks$entityPierce;
    }

    @Override
    public void tacztweaks$incrementEntityPierce() {
        tacztweaks$entityPierce++;
    }

    @Override
    public Vec3 tacztweaks$getPosition() {
        return tacztweaks$position;
    }

    @Override
    public void tacztweaks$setPosition(Vec3 position) {
        tacztweaks$position = position;
    }

    @Override
    public void tacztweaks$addDamageModifier(float flat, float multiplier) {
        tacztweaks$damageModifiers.add(new DamageModifier(flat, multiplier));
    }

    @Override
    public void tacztweaks$popDamageModifier() {
        if (!tacztweaks$damageModifiers.isEmpty()) tacztweaks$damageModifiers.remove(tacztweaks$damageModifiers.size() - 1);
    }

    @Override
    public int tacztweaks$getBurstIndex() {
        return tacztweaks$burstIndex;
    }

    @Override
    public void tacztweaks$setBurstIndex(int index) {
        tacztweaks$burstIndex = index;
    }

    @Override
    public int tacztweaks$getPelletIndex() {
        return tacztweaks$pelletIndex;
    }

    @Override
    public void tacztweaks$setPelletIndex(int index) {
        tacztweaks$pelletIndex = index;
    }

    private static final String INIT = "(Lnet/minecraft/class_1299;Lnet/minecraft/class_1937;Lnet/minecraft/class_1309;Lnet/minecraft/class_1799;Lnet/minecraft/class_2960;Lnet/minecraft/class_2960;Lnet/minecraft/class_2960;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V";

    @Inject(method = "<init>" + INIT, at = @At("RETURN"))
    private void tacztweaks$init(EntityType<?> type, Level worldIn, LivingEntity throwerIn, ItemStack gunItem, Identifier ammoId, Identifier gunId, Identifier gunDisplayId, boolean isTracerAmmo, GunData gunData, BulletData bulletData, CallbackInfo ci) {
        tacztweaks$gunStack = gunItem;
    }

    @ModifyExpressionValue(method = "getDamage", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/ExtraDamage$DistanceDamagePair;getDamage()F"))
    private float tacztweaks$getDamage$applyDamageModifiers(float original) {
        float damage = original;
        for (DamageModifier modifier : tacztweaks$damageModifiers) {
            damage = (damage + modifier.flat()) * modifier.multiplier();
        }
        return damage;
    }

    @WrapOperation(method = "onBulletTick", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;onHitEntity(Lcom/tacz/guns/util/TacHitResult;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V", remap = true))
    private void tacztweaks$onBulletTick$onHitEntity(EntityKineticBullet self, TacHitResult result, Vec3 from, Vec3 to, Operation<Void> original) {
        Entity entity = result.getEntity();
        if (entity instanceof ServerPlayer player) tacztweaks$hitPlayers.add(player);

        var damage = BulletInteractionManager.INSTANCE.getEntityDamage(self, result.getLocation(), entity);
        if (damage != null) tacztweaks$addDamageModifier(damage.getFirst(), damage.getSecond());
        try {
            original.call(self, result, from, to);
        } finally {
            if (damage != null) tacztweaks$popDamageModifier();
        }

        if (self.level() instanceof ServerLevel level) {
            boolean dead = !entity.isAlive();
            BulletSoundsManager.INSTANCE.handleEntitySound(
                dead ? BulletSoundsManager.EEntitySoundType.KILL : BulletSoundsManager.EEntitySoundType.HIT,
                level, self, result.getLocation(), entity
            );
            BulletParticlesManager.INSTANCE.handleEntityParticle(
                dead ? BulletParticlesManager.EEntityParticleType.KILL : BulletParticlesManager.EEntityParticleType.HIT,
                level, self, result.getLocation(), entity
            );
        }
    }

    @Inject(method = "method_5773", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;onBulletTick()V", shift = At.Shift.AFTER))
    private void tacztweaks$tick$handleSounds(CallbackInfo ci) {
        EntityKineticBullet self = (EntityKineticBullet) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) return;
        BulletSoundsManager.INSTANCE.handleConstant(level, self);
        BulletSoundsManager.INSTANCE.handleSoundWhizz(level, self, tacztweaks$hitPlayers, tacztweaks$whizzedPlayers);
    }
}
