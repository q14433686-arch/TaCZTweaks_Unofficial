package me.muksc.tacztweaks.mixin.tweaks;

import me.muksc.tacztweaks.mixininterface.tweaks.TaCZIdentifier;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * Adds the mono-audio flag to {@link Identifier}.
 *
 * <p>The 26.2 port note that "Identifier is a record and cannot be mixed into"
 * is <b>incorrect</b>: {@code Identifier} is a plain {@code public final class}
 * (with {@code getNamespace()}/{@code getPath()} accessors, exactly like the old
 * {@code ResourceLocation}), so the upstream {@code ResourceLocationMixin} ports
 * directly with only the rename.</p>
 */
@Mixin(Identifier.class)
public abstract class IdentifierMixin implements TaCZIdentifier {
    @Unique
    private boolean tacztweaks$monoAudio = false;

    @Override
    public boolean tacztweaks$getMonoAudio() {
        return tacztweaks$monoAudio;
    }

    @Override
    public void tacztweaks$setMonoAudio(boolean monoAudio) {
        tacztweaks$monoAudio = monoAudio;
    }
}
