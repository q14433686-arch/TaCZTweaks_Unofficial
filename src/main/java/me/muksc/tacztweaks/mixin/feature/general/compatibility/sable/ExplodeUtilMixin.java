package me.muksc.tacztweaks.mixin.feature.general.compatibility.sable;

import com.tacz.guns.util.ExplodeUtil;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ExplodeUtil.class, remap = false)
public abstract class ExplodeUtilMixin { }
