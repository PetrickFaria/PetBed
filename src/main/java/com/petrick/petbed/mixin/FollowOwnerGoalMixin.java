package com.petrick.petbed.mixin;

import com.petrick.petbed.block.PetLinkService;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Conditionally pauses only the vanilla owner-follow goal for an animal carrying a PetBed link. */
@Mixin(FollowOwnerGoal.class)
public abstract class FollowOwnerGoalMixin {
    @Shadow @Final private TamableAnimal tamable;

    @Inject(method = "canUse", at = @At("HEAD"), cancellable = true)
    private void petbed$skipOwnerFollowWhenLinked(CallbackInfoReturnable<Boolean> callback) {
        if (PetLinkService.hasLinkMarker(tamable)) callback.setReturnValue(false);
    }

    @Inject(method = "canContinueToUse", at = @At("HEAD"), cancellable = true)
    private void petbed$stopOwnerFollowWhenLinked(CallbackInfoReturnable<Boolean> callback) {
        if (PetLinkService.hasLinkMarker(tamable)) callback.setReturnValue(false);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void petbed$cancelOwnerFollowTickWhenLinked(CallbackInfo callback) {
        if (PetLinkService.hasLinkMarker(tamable)) callback.cancel();
    }
}
