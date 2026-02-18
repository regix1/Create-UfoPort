package io.github.fabricators_of_create.porting_lib.mixin.common;

import net.minecraft.world.item.Tier;

import org.spongepowered.asm.mixin.Mixin;

import io.github.fabricators_of_create.porting_lib.extensions.extensions.TierExtensions;

@Mixin(Tier.class)
public interface TierMixin extends TierExtensions {
}
