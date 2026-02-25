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

/**
 * Renders Cobblemon Pokemon portraits via reflection so that Cobblemon remains
 * an optional (soft) dependency.
 * <p>
 * Calls {@code com.cobblemon.mod.common.api.gui.GuiUtilsKt.drawPosablePortrait},
 * which is a Kotlin top-level function compiled to a static method. The 7-param
 * {@code @JvmOverloads} overload is used:
 * {@code (ResourceLocation, PoseStack, float scale, float contextScale,
 *         boolean reversed, PosableState state, float partialTicks)}
 * <p>
 * Internally, {@code drawPosablePortrait} translates Y by
 * {@code BattleOverlay.PORTRAIT_DIAMETER + 2} (= 30 pixels) and scales by
 * the requested {@code scale} factor, so the caller must position the
 * PoseStack so that the top-left of the portrait area is at the origin.
 */
@Environment(EnvType.CLIENT)
public class CobblemonPortraitRenderer {

	private static final Logger LOGGER = LogUtils.getLogger();

	/** Cobblemon's internal portrait diameter from BattleOverlay.PORTRAIT_DIAMETER. */
	private static final int COBBLEMON_PORTRAIT_DIAMETER = 28;

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
	private static Method getResourceIdentifierMethod;

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

			// 7-param @JvmOverloads overload (Cobblemon 1.7.x):
			// drawPosablePortrait(ResourceLocation, PoseStack, float, float, boolean, PosableState, float)
			// The remaining params (limbSwing, limbSwingAmount, ageInTicks, headYaw, headPitch,
			// doQuirks, r, g, b, a) all use their defaults.
			drawPortraitMethod = guiUtilsClass.getMethod("drawPosablePortrait",
					ResourceLocation.class, PoseStack.class,
					float.class, float.class, boolean.class,
					posableStateClass, float.class);

			setAspectsMethod = posableStateClass.getMethod("setCurrentAspects", Set.class);

			// NOTE: The correct fully-qualified name is api.pokemon, NOT pokemon.PokemonSpecies
			Class<?> pokemonSpeciesClass = Class.forName("com.cobblemon.mod.common.api.pokemon.PokemonSpecies");
			speciesRegistryInstance = pokemonSpeciesClass.getField("INSTANCE").get(null);
			getByNameMethod = speciesRegistryInstance.getClass().getMethod("getByName", String.class);

			LOGGER.info("Cobblemon portrait rendering initialized successfully");
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
			LOGGER.warn("Failed to get base scale for {}", speciesName, e);
			return 1.0f;
		}
	}

	/**
	 * Renders a Pokemon portrait at the given position.
	 * <p>
	 * The portrait is rendered similarly to how Cobblemon's own PartyOverlay does it:
	 * translate the PoseStack so the portrait area's top-center is at the origin,
	 * offset upward by 12 pixels, then call drawPosablePortrait which handles the
	 * internal scaling and model positioning.
	 *
	 * @param graphics     the current GuiGraphics context
	 * @param speciesName  species name (e.g. "Bulbasaur") - will be lowercased for ResourceLocation
	 * @param state        a FloatingState/PosableState previously created via createState()
	 * @param portraitX    the left edge of the portrait area (pixels)
	 * @param portraitY    the top edge of the portrait area (pixels)
	 * @param portraitSize the width/height of the portrait area (pixels)
	 * @param contextScale the species base scale (from getBaseScale)
	 * @param partialTicks current partial tick
	 */
	private static ResourceLocation resolveSpeciesIdentifier(String speciesName) {
		try {
			Object species = getByNameMethod.invoke(speciesRegistryInstance, speciesName.toLowerCase());
			if (species != null) {
				if (getResourceIdentifierMethod == null)
					getResourceIdentifierMethod = species.getClass().getMethod("getResourceIdentifier");
				Object identifier = getResourceIdentifierMethod.invoke(species);
				if (identifier instanceof ResourceLocation rl)
					return rl;
			}
		} catch (Exception e) {
			LOGGER.warn("Failed to resolve species identifier for {}", speciesName, e);
		}
		return null;
	}

	public static void renderPortrait(GuiGraphics graphics, String speciesName,
			Object state, int portraitX, int portraitY, int portraitSize,
			float contextScale, float partialTicks) {
		if (!ensureInitialized() || state == null)
			return;

		try {
			ResourceLocation identifier = resolveSpeciesIdentifier(speciesName);
			if (identifier == null)
				return;

			PoseStack poseStack = graphics.pose();

			poseStack.pushPose();

			// Match how Cobblemon's PartyOverlay positions the portrait:
			// Translate to (centerX, topY - vertical_offset, 0)
			// drawPosablePortrait internally translates Y by PORTRAIT_DIAMETER+2 (=30)
			// and applies the scale, model portrait offsets, rotation, and lighting.
			poseStack.translate(
					portraitX + portraitSize / 2.0 - 1.0,
					portraitY - 12.0,
					0.0);

			drawPortraitMethod.invoke(null,
					identifier, poseStack, 13.0f, contextScale, false, state, partialTicks);

			poseStack.popPose();
		} catch (Exception e) {
			LOGGER.warn("Failed to render portrait for {}", speciesName, e);
		}
	}

	public static boolean isAvailable() {
		return ensureInitialized();
	}
}
