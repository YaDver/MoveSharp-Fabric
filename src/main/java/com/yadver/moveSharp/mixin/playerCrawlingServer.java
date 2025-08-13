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

    @Inject(require = 1, method = "initDataTracker", at = @At("HEAD"))
    public void onInitDataTracker(CallbackInfo ci) {
        getDataTracker().startTracking(MoveSharp.IS_CRAWLING, false);
    }

    @Inject(method = "updatePose", at = @At("TAIL"))
    private void updatePose(CallbackInfo ci) {
        if (this.wouldPoseNotCollide(EntityPose.SWIMMING)
//                && this.getDataTracker().containsKey(MoveSharp.IS_CRAWLING)
                && this.getDataTracker().get(MoveSharp.IS_CRAWLING)
        ) {
            this.setPose(EntityPose.SWIMMING);
        }
    }
}