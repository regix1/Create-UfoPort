package com.simibubi.create.compat.cobblemon;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

import org.slf4j.Logger;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.logging.LogUtils;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

@Environment(EnvType.CLIENT)
public class CobblemonPortraitRenderer {

	private static final Logger LOGGER = LogUtils.getLogger();

	private static final int PORTRAIT_DIAMETER = 39;

	private static boolean initialized = false;
	private static boolean available = false;

	private static Method drawPortraitMethod;
	private static Constructor<?> floatingStateConstructor;
	private static Method setAspectsMethod;
	private static Class<?> posableStateClass;

	private static Object speciesRegistryInstance;
	private static Method getByNameMethod;
	private static Method getStandardFormMethod;
	private static Method getBaseScaleMethod;

	private static synchronized boolean ensureInitialized() {
		if (initialized)
			return available;
		initialized = true;

		try {
			Class<?> guiUtilsClass = Class.forName("com.cobblemon.mod.common.api.gui.GuiUtilsKt");
			posableStateClass = Class.forName(
					"com.cobblemon.mod.common.client.render.models.blockbench.PosableState");
			Class<?> floatingStateClass = Class.forName(
					"com.cobblemon.mod.common.client.render.models.blockbench.FloatingState");

			floatingStateConstructor = floatingStateClass.getConstructor();

			// 7-param @JvmOverloads overload:
			// (ResourceLocation, PoseStack, float, float, boolean, PosableState, float)
			drawPortraitMethod = guiUtilsClass.getMethod("drawPosablePortrait",
					ResourceLocation.class, PoseStack.class,
					float.class, float.class, boolean.class,
					posableStateClass, float.class);

			setAspectsMethod = posableStateClass.getMethod("setCurrentAspects", Set.class);

			Class<?> pokemonSpeciesClass = Class.forName("com.cobblemon.mod.common.pokemon.PokemonSpecies");
			speciesRegistryInstance = pokemonSpeciesClass.getField("INSTANCE").get(null);
			getByNameMethod = speciesRegistryInstance.getClass().getMethod("getByName", String.class);

			available = true;
		} catch (Exception e) {
			LOGGER.warn("Cobblemon portrait rendering not available: {}", e.getMessage());
			available = false;
		}

		return available;
	}

	public static Object createState(Set<String> aspects) {
		if (!ensureInitialized())
			return null;
		try {
			Object state = floatingStateConstructor.newInstance();
			if (aspects != null && !aspects.isEmpty()) {
				setAspectsMethod.invoke(state, aspects);
			}
			return state;
		} catch (Exception e) {
			LOGGER.warn("Failed to create FloatingState", e);
			return null;
		}
	}

	public static float getBaseScale(String speciesName) {
		if (!ensureInitialized())
			return 1.0f;
		try {
			Object species = getByNameMethod.invoke(speciesRegistryInstance, speciesName.toLowerCase());
			if (species == null)
				return 1.0f;

			if (getStandardFormMethod == null)
				getStandardFormMethod = species.getClass().getMethod("getStandardForm");
			Object form = getStandardFormMethod.invoke(species);

			if (getBaseScaleMethod == null)
				getBaseScaleMethod = form.getClass().getMethod("getBaseScale");
			return (float) getBaseScaleMethod.invoke(form);
		} catch (Exception e) {
			return 1.0f;
		}
	}

	public static void renderPortrait(GuiGraphics graphics, String speciesName,
			Object state, int centerX, int centerY,
			float scale, float contextScale, float partialTicks) {
		if (!ensureInitialized() || state == null)
			return;

		try {
			ResourceLocation identifier = ResourceLocation.fromNamespaceAndPath(
					"cobblemon", speciesName.toLowerCase());

			PoseStack poseStack = graphics.pose();

			poseStack.pushPose();
			// drawPosablePortrait internally translates by (PORTRAIT_DIAMETER+2, PORTRAIT_DIAMETER-2)
			// so offset our position to center the portrait at the desired point
			poseStack.translate(
					centerX - PORTRAIT_DIAMETER - 2,
					centerY - PORTRAIT_DIAMETER + 2,
					100);

			drawPortraitMethod.invoke(null,
					identifier, poseStack, scale, contextScale, false, state, partialTicks);

			poseStack.popPose();
		} catch (Exception e) {
			// Silent fail - text-only fallback
		}
	}

	public static boolean isAvailable() {
		return ensureInitialized();
	}
}
