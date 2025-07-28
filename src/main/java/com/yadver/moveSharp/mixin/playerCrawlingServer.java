package com.yadver.moveSharp.mixin;

import com.yadver.moveSharp.MoveSharp;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public abstract class playerCrawlingServer extends LivingEntity {
    protected playerCrawlingServer(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }

    @Inject(method = "updatePose", at = @At("TAIL"))
    private void updatePose(CallbackInfo ci) {
        if (this.wouldPoseNotCollide(EntityPose.SWIMMING)) {
            if (MoveSharp.isCrawling) {
                this.setPose(EntityPose.SWIMMING);
            }
        }
    }
}