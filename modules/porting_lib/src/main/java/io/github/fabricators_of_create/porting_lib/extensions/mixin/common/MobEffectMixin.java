package io.github.fabricators_of_create.porting_lib.extensions.mixin.common;

import net.minecraft.world.effect.MobEffect;

import org.spongepowered.asm.mixin.Mixin;

import io.github.fabricators_of_create.porting_lib.extensions.extensions.MobEffectExtensions;

@Mixin(MobEffect.class)
public class MobEffectMixin implements MobEffectExtensions {
}
