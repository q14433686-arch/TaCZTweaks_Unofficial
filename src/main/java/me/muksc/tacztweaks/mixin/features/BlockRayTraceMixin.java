package me.muksc.tacztweaks.mixin.features;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.util.block.BlockRayTrace;
import me.muksc.tacztweaks.core.BulletRayTracer;
import me.muksc.tacztweaks.mixin.accessor.ClipContextAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercepts TaCZ's bullet block ray-trace so data-driven bullet interactions can
 * break blocks and let bullets pierce through them.
 *
 * 26.1.2 note: the upstream target was {@code lambda$rayTraceBlocks$1/2}; the refabricated
 * port renamed the per-block handler to the stable hook {@code getBlockHitResult} (whose
 * parameters already carry the block state), so this mixin targets that method instead.
 * Returning null from it makes {@code performRayTrace} skip the block and keep tracing —
 * which is exactly the "pierce" semantics.
 */
@Mixin(value = BlockRayTrace.class, remap = false)
public abstract class BlockRayTraceMixin {
    @Unique
    private static final ThreadLocal<BulletRayTracer> tacztweaks$rayTracer = new ThreadLocal<>();

    @Inject(method = "rayTraceBlocks", at = @At("HEAD"))
    private static void tacztweaks$rayTraceBlocks$init(Level level, ClipContext context, CallbackInfoReturnable<BlockHitResult> cir) {
        tacztweaks$rayTracer.remove();
        if (!(level instanceof ServerLevel serverLevel)) return;
        ClipContextAccessor accessor = (ClipContextAccessor) context;
        if (!(accessor.getCollisionContext() instanceof EntityCollisionContext entityCollisionContext)) return;
        if (!(entityCollisionContext.getEntity() instanceof EntityKineticBullet entity)) return;
        tacztweaks$rayTracer.set(new BulletRayTracer(entity, serverLevel));
    }

    @ModifyReturnValue(method = "getBlockHitResult", at = @At("RETURN"))
    private static BlockHitResult tacztweaks$getBlockHitResult$handle(BlockHitResult original, Level level, ClipContext context, BlockPos pos, BlockState state) {
        BulletRayTracer rayTracer = tacztweaks$rayTracer.get();
        if (rayTracer == null) return original;
        if (original == null || original.getType() == HitResult.Type.MISS) return original;
        if (state == null || state.isAir()) return original;
        return rayTracer.handle(original, state);
    }

    @Inject(method = "rayTraceBlocks", at = @At("RETURN"))
    private static void tacztweaks$rayTraceBlocks$clear(Level level, ClipContext context, CallbackInfoReturnable<BlockHitResult> cir) {
        tacztweaks$rayTracer.remove();
    }
}
