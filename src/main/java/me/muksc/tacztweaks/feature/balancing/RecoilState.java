package me.muksc.tacztweaks.feature.balancing;

import com.tacz.guns.api.entity.IGunOperator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Pose;

/** Snapshot of player state used while constructing one recoil spline. */
public record RecoilState(boolean crawling, float aimingProgress) {
    public static RecoilState capture() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return new RecoilState(false, 0.0F);
        boolean crawling = !player.isSwimming() && player.getPose() == Pose.SWIMMING;
        float aiming = IGunOperator.fromLivingEntity(player).getSynAimingProgress();
        return new RecoilState(crawling, aiming);
    }
}
