package com.simibubi.create.compat.cobblemon;

import java.util.Set;

import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPoint;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmInteractionPointType;
import com.simibubi.create.content.kinetics.mechanicalArm.AllArmInteractionPointTypes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CobblemonCompat {

	private static final Set<String> DEPOSIT_ONLY_BLOCKS = Set.of(
		"cobblemon:fossil_analyzer",
		"cobblemon:restoration_tank"
	);

	private static final Set<String> INTERACT_BLOCKS = Set.of(
		"cobblemon:display_case",
		"cobblemon:gilded_chest",
		"cobblemon:blue_gilded_chest",
		"cobblemon:yellow_gilded_chest",
		"cobblemon:pink_gilded_chest",
		"cobblemon:black_gilded_chest",
		"cobblemon:white_gilded_chest",
		"cobblemon:green_gilded_chest",
		"cobblemon:gimmighoul_chest"
	);

	public static void init() {
		registerArmInteractionTypes();
	}

	private static void registerArmInteractionTypes() {
		ResourceLocation depositId = ResourceLocation.fromNamespaceAndPath("create", "cobblemon_deposit");
		ArmInteractionPointType.register(new CobblemonDepositType(depositId));

		ResourceLocation interactId = ResourceLocation.fromNamespaceAndPath("create", "cobblemon_interact");
		ArmInteractionPointType.register(new CobblemonInteractType(interactId));
	}

	private static String getBlockId(BlockState state) {
		return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
	}

	public static class CobblemonDepositType extends ArmInteractionPointType {
		public CobblemonDepositType(ResourceLocation id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
			return DEPOSIT_ONLY_BLOCKS.contains(getBlockId(state));
		}

		@Override
		public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
			return new AllArmInteractionPointTypes.DepositOnlyArmInteractionPoint(this, level, pos, state);
		}
	}

	public static class CobblemonInteractType extends ArmInteractionPointType {
		public CobblemonInteractType(ResourceLocation id) {
			super(id);
		}

		@Override
		public boolean canCreatePoint(Level level, BlockPos pos, BlockState state) {
			return INTERACT_BLOCKS.contains(getBlockId(state));
		}

		@Override
		public ArmInteractionPoint createPoint(Level level, BlockPos pos, BlockState state) {
			return new ArmInteractionPoint(this, level, pos, state);
		}
	}
}
