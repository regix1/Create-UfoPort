package io.github.fabricators_of_create.porting_lib.entity.mixin.common;

import net.minecraft.world.item.Item;

import org.spongepowered.asm.mixin.Mixin;

import io.github.fabricators_of_create.porting_lib.entity.extensions.ItemExtensions;

@Mixin(Item.class)
public class ItemMixin implements ItemExtensions {
}
