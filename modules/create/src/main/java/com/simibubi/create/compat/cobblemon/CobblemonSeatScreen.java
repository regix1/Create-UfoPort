package com.simibubi.create.compat.cobblemon;

import java.util.HashSet;
import java.util.List;

import com.simibubi.create.AllPackets;
import com.simibubi.create.compat.cobblemon.CobblemonCompat.PartySlotData;
import com.simibubi.create.foundation.gui.AbstractSimiScreen;
import com.simibubi.create.foundation.utility.Lang;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;

@Environment(EnvType.CLIENT)
public class CobblemonSeatScreen extends AbstractSimiScreen {

	private static final int SLOT_WIDTH = 200;
	private static final int SLOT_HEIGHT = 44;
	private static final int SLOT_GAP = 4;
	private static final int PADDING = 10;
	private static final int TITLE_HEIGHT = 20;
	private static final int PORTRAIT_SIZE = 40;

	private static final int BG_COLOR = 0xCC000000;
	private static final int SLOT_COLOR = 0xFF2A2A3A;
	private static final int SLOT_HOVER_COLOR = 0xFF3A3A5A;
	private static final int SLOT_BORDER = 0xFF5391E1;
	private static final int PORTRAIT_BG = 0xFF1A1A2A;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int LEVEL_COLOR = 0xFFAAAABB;
	private static final int TITLE_COLOR = 0xFF5391E1;

	private final BlockPos seatPos;
	private final List<PartySlotData> partyData;

	private Object[] portraitStates;
	private float[] baseScales;
	private boolean portraitsAvailable;

	public CobblemonSeatScreen(BlockPos seatPos, List<PartySlotData> partyData) {
		super(Lang.translateDirect("pokemon_seat.title"));
		this.seatPos = seatPos;
		this.partyData = partyData;
	}

	@Override
	protected void init() {
		int presentCount = (int) partyData.stream().filter(PartySlotData::present).count();
		int contentHeight = TITLE_HEIGHT + presentCount * (SLOT_HEIGHT + SLOT_GAP) - SLOT_GAP + PADDING * 2;
		setWindowSize(SLOT_WIDTH + PADDING * 2, contentHeight);
		super.init();

		portraitsAvailable = CobblemonPortraitRenderer.isAvailable();
		portraitStates = new Object[partyData.size()];
		baseScales = new float[partyData.size()];

		if (portraitsAvailable) {
			for (int i = 0; i < partyData.size(); i++) {
				PartySlotData slot = partyData.get(i);
				if (slot.present()) {
					portraitStates[i] = CobblemonPortraitRenderer.createState(
							new HashSet<>(slot.aspects()));
					baseScales[i] = CobblemonPortraitRenderer.getBaseScale(slot.speciesName());
				}
			}
		}
	}

	@Override
	protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		int presentCount = (int) partyData.stream().filter(PartySlotData::present).count();
		int contentHeight = TITLE_HEIGHT + presentCount * (SLOT_HEIGHT + SLOT_GAP) - SLOT_GAP + PADDING * 2;

		graphics.fill(guiLeft, guiTop, guiLeft + windowWidth, guiTop + contentHeight, BG_COLOR);
		renderBorder(graphics, guiLeft, guiTop, windowWidth, contentHeight, SLOT_BORDER);

		graphics.drawCenteredString(font, title, guiLeft + windowWidth / 2, guiTop + PADDING, TITLE_COLOR);

		int slotY = guiTop + PADDING + TITLE_HEIGHT;
		for (int i = 0; i < partyData.size(); i++) {
			PartySlotData slot = partyData.get(i);
			if (!slot.present())
				continue;

			int slotX = guiLeft + PADDING;
			boolean hovered = mouseX >= slotX && mouseX < slotX + SLOT_WIDTH
					&& mouseY >= slotY && mouseY < slotY + SLOT_HEIGHT;

			int bgColor = hovered ? SLOT_HOVER_COLOR : SLOT_COLOR;
			graphics.fill(slotX, slotY, slotX + SLOT_WIDTH, slotY + SLOT_HEIGHT, bgColor);

			if (hovered)
				renderBorder(graphics, slotX, slotY, SLOT_WIDTH, SLOT_HEIGHT, SLOT_BORDER);

			// Portrait background
			int portraitX = slotX + 2;
			int portraitY = slotY + 2;
			graphics.fill(portraitX, portraitY,
					portraitX + PORTRAIT_SIZE, portraitY + PORTRAIT_SIZE, PORTRAIT_BG);

			// Render Pokemon portrait with scissoring
			if (portraitsAvailable && portraitStates[i] != null) {
				int portraitCenterX = portraitX + PORTRAIT_SIZE / 2;
				int portraitCenterY = portraitY + PORTRAIT_SIZE / 2;

				graphics.enableScissor(portraitX, portraitY,
						portraitX + PORTRAIT_SIZE, portraitY + PORTRAIT_SIZE);
				CobblemonPortraitRenderer.renderPortrait(
						graphics, slot.speciesName(), portraitStates[i],
						portraitCenterX, portraitCenterY,
						13.0f, baseScales[i], partialTicks);
				graphics.disableScissor();
			}

			// Pokemon name and level
			int textX = slotX + PORTRAIT_SIZE + 10;
			String name = capitalize(slot.speciesName());
			String level = "Lv." + slot.level();

			graphics.drawString(font, name, textX, slotY + (SLOT_HEIGHT - 8) / 2, TEXT_COLOR);
			int levelWidth = font.width(level);
			graphics.drawString(font, level, slotX + SLOT_WIDTH - 8 - levelWidth,
					slotY + (SLOT_HEIGHT - 8) / 2, LEVEL_COLOR);

			slotY += SLOT_HEIGHT + SLOT_GAP;
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		int slotY = guiTop + PADDING + TITLE_HEIGHT;
		for (int i = 0; i < partyData.size(); i++) {
			PartySlotData slot = partyData.get(i);
			if (!slot.present())
				continue;

			int slotX = guiLeft + PADDING;
			if (mouseX >= slotX && mouseX < slotX + SLOT_WIDTH
					&& mouseY >= slotY && mouseY < slotY + SLOT_HEIGHT) {
				AllPackets.getChannel().sendToServer(new CobblemonSeatPokemonPacket(seatPos, i));
				onClose();
				return true;
			}

			slotY += SLOT_HEIGHT + SLOT_GAP;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	private void renderBorder(GuiGraphics graphics, int x, int y, int w, int h, int color) {
		graphics.fill(x, y, x + w, y + 1, color);
		graphics.fill(x, y + h - 1, x + w, y + h, color);
		graphics.fill(x, y, x + 1, y + h, color);
		graphics.fill(x + w - 1, y, x + w, y + h, color);
	}

	private static String capitalize(String str) {
		if (str == null || str.isEmpty())
			return str;
		return Character.toUpperCase(str.charAt(0)) + str.substring(1).toLowerCase();
	}
}
