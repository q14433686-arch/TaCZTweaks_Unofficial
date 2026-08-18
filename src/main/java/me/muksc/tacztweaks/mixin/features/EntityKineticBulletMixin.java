package me.muksc.tacztweaks.mixin.features;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.TacHitResult;
import me.muksc.tacztweaks.compat.FirstAidCompat;
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
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Injects the state and hooks needed by the data-driven bullet interaction system into
 * {@code EntityKineticBullet}:
 * <ul>
 *   <li>stores the gun item stack (for armor-ignore / gun lookups);</li>
 *   <li>applies the per-hit damage modifiers ({@code pierce} damage falloff / entity damage);</li>
 *   <li>wraps {@code onHitEntity} to apply entity interactions and play hit/kill sounds & particles;</li>
 *   <li>after each tick, plays constant / whizz sounds.</li>
 * </ul>
 */
@Mixin(value = EntityKineticBullet.class, remap = false)
public abstract class EntityKineticBulletMixin implements EntityKineticBulletExtension {
    @Shadow
    private int pierce;

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
    private boolean tacztweaks$positionUpdatedThisTick;

    @Unique
    private final List<ServerPlayer> tacztweaks$hitPlayers = new ArrayList<>();

    @Unique
    private final Set<UUID> tacztweaks$whizzedPlayers = new HashSet<>();

    @Unique
    private int tacztweaks$burstIndex;

    @Unique
    private int tacztweaks$pelletIndex;

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
    public int tacztweaks$getGunPierce() {
        return pierce;
    }

    @Override
    public void tacztweaks$setGunPierce(int value) {
        pierce = value;
    }

    @Override
    public void tacztweaks$incrementGunPierce() {
        pierce++;
    }

    @Override
    public void tacztweaks$decrementGunPierce() {
        pierce--;
    }

    @Override
    public Vec3 tacztweaks$getPosition() {
        return tacztweaks$position;
    }

    @Override
    public void tacztweaks$setPosition(Vec3 position) {
        tacztweaks$position = position;
        tacztweaks$positionUpdatedThisTick = true;
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

    @Override
    public void tacztweaks$addDamageModifier(float flat, float multiplier) {
        tacztweaks$damageModifiers.add(new DamageModifier(flat, multiplier));
    }

    @Override
    public void tacztweaks$popDamageModifier() {
        if (!tacztweaks$damageModifiers.isEmpty()) tacztweaks$damageModifiers.remove(tacztweaks$damageModifiers.size() - 1);
    }

    private static final String INIT = "(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/resources/Identifier;Lnet/minecraft/resources/Identifier;Lnet/minecraft/resources/Identifier;ZLcom/tacz/guns/resource/pojo/data/gun/GunData;Lcom/tacz/guns/resource/pojo/data/gun/BulletData;)V";

    @Inject(method = "<init>" + INIT, at = @At("RETURN"))
    private void tacztweaks$init(EntityType<?> type, Level worldIn, LivingEntity throwerIn, ItemStack gunItem, Identifier ammoId, Identifier gunId, Identifier gunDisplayId, boolean isTracerAmmo, GunData gunData, BulletData bulletData, CallbackInfo ci) {
        tacztweaks$gunStack = gunItem;
    }

    @ModifyExpressionValue(
        method = "onBulletTick",
        at = @At(
            value = "FIELD",
            target = "Lcom/tacz/guns/entity/EntityKineticBullet;pierce:I",
            ordinal = 0
        )
    )
    private int tacztweaks$onBulletTick$traceCustomPierce(int original) {
        return BulletInteractionManager.INSTANCE.needsExtendedEntityTrace()
            ? Math.max(2, original)
            : original;
    }

    @ModifyExpressionValue(
        method = "onBulletTick",
        at = @At(
            value = "INVOKE",
            target = "Lcom/tacz/guns/util/EntityUtil;findEntitiesOnPath(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)Ljava/util/List;"
        )
    )
    private List<EntityKineticBullet.EntityResult> tacztweaks$onBulletTick$sortAllEntityHits(
        List<EntityKineticBullet.EntityResult> original
    ) {
        Vec3 start = ((EntityKineticBullet) (Object) this).position();
        original.sort(Comparator.comparingDouble(result -> result.getHitPos().distanceToSqr(start)));
        return original;
    }

    @ModifyExpressionValue(method = "getDamage", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/ExtraDamage$DistanceDamagePair;getDamage()F"))
    private float tacztweaks$getDamage$applyDamageModifiers(float original) {
        float damage = original;
        for (DamageModifier modifier : tacztweaks$damageModifiers) {
            damage = (damage + modifier.flat()) * modifier.multiplier();
        }
        return damage;
    }

    @WrapOperation(method = "onBulletTick", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;onHitEntity(Lcom/tacz/guns/util/TacHitResult;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;)V"))
    private void tacztweaks$onBulletTick$onHitEntity(EntityKineticBullet self, TacHitResult result, Vec3 from, Vec3 to, Operation<Void> original) {
        FirstAidCompat.recordProjectileHit(self, result);
        tacztweaks$setPosition(result.getLocation());
        Entity entity = result.getEntity();
        if (entity instanceof ServerPlayer player) tacztweaks$hitPlayers.add(player);

        BulletInteractionManager.EntityInteraction interaction =
            BulletInteractionManager.INSTANCE.prepareEntityInteraction(self, result.getLocation(), entity);
        tacztweaks$addDamageModifier(interaction.getDamageModifier(), interaction.getDamageMultiplier());
        try {
            original.call(self, result, from, to);
        } finally {
            tacztweaks$popDamageModifier();
        }

        boolean dead = !entity.isAlive();
        BulletInteractionManager.EntityInteractionResult interactionResult =
            BulletInteractionManager.INSTANCE.finishEntityInteraction(
                self, result.getLocation(), interaction, dead
            );
        if (!interactionResult.getPierce()) {
            // The caller decrements immediately after this wrapper. Setting one makes it
            // reach zero and follow its normal stop/discard path.
            tacztweaks$setGunPierce(1);
        } else if (!interactionResult.getConsumeGunPierce()) {
            // Compensate the caller's unconditional native decrement.
            tacztweaks$incrementGunPierce();
        }

        if (self.level() instanceof ServerLevel level) {
            BulletSoundsManager.EEntitySoundType soundType = dead
                ? BulletSoundsManager.EEntitySoundType.KILL
                : interactionResult.getPierce()
                    ? BulletSoundsManager.EEntitySoundType.PIERCE
                    : BulletSoundsManager.EEntitySoundType.HIT;
            BulletParticlesManager.EEntityParticleType particleType = dead
                ? BulletParticlesManager.EEntityParticleType.KILL
                : interactionResult.getPierce()
                    ? BulletParticlesManager.EEntityParticleType.PIERCE
                    : BulletParticlesManager.EEntityParticleType.HIT;
            BulletSoundsManager.INSTANCE.handleEntitySound(
                soundType, level, self, result.getLocation(), entity
            );
            BulletParticlesManager.INSTANCE.handleEntityParticle(
                particleType, level, self, result.getLocation(), entity
            );
        }
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;onBulletTick()V"))
    private void tacztweaks$tick$resetDestination(CallbackInfo ci) {
        tacztweaks$positionUpdatedThisTick = false;
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/entity/EntityKineticBullet;onBulletTick()V", shift = At.Shift.AFTER))
    private void tacztweaks$tick$handleSounds(CallbackInfo ci) {
        EntityKineticBullet self = (EntityKineticBullet) (Object) this;
        if (!(self.level() instanceof ServerLevel level)) return;
        if (!tacztweaks$positionUpdatedThisTick) {
            tacztweaks$position = self.position().add(self.getDeltaMovement());
        }
        BulletSoundsManager.INSTANCE.handleConstant(level, self);
        BulletSoundsManager.INSTANCE.handleSoundWhizz(
            level, self, tacztweaks$hitPlayers, tacztweaks$whizzedPlayers
        );
    }
}
