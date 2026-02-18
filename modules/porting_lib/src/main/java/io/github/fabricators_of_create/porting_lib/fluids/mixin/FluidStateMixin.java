package io.github.fabricators_of_create.porting_lib.fluids.mixin;

import net.minecraft.world.level.material.FluidState;

import org.spongepowered.asm.mixin.Mixin;

import io.github.fabricators_of_create.porting_lib.fluids.extensions.FluidStateExtension;

@Mixin(FluidState.class)
public class FluidStateMixin implements FluidStateExtension {
}
