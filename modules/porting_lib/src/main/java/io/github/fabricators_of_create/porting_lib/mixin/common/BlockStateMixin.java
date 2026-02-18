package io.github.fabricators_of_create.porting_lib.mixin.common;

import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;

import io.github.fabricators_of_create.porting_lib.extensions.BaseBlockStateExtension;
import io.github.fabricators_of_create.porting_lib.extensions.extensions.BlockStateExtensions;

@Mixin(BlockState.class)
public class BlockStateMixin implements BlockStateExtensions, BaseBlockStateExtension {
}
