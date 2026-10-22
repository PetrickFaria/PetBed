package com.petrick.petbed.mixin;

import com.petrick.petbed.PetRestGoal;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Disables entity-to-entity pushing only while a linked pet is resting on its bed. */
@Mixin(Entity.class)
public abstract class RestingPetCollisionMixin {
    @Inject(method = "isPushable", at = @At("HEAD"), cancellable = true)
    private void petbed$restingPetCannotBePushed(CallbackInfoReturnable<Boolean> callback) {
        if (PetRestGoal.isRestingLinkedPet((Entity) (Object) this)) callback.setReturnValue(false);
    }
}
