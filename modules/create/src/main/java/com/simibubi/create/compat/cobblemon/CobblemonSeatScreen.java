package com.simibubi.create.compat.cobblemon;

import java.util.ArrayList;
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
	private static final int TAB_HEIGHT = 20;
	private static final int TAB_GAP = 4;
	private static final int NAV_BAR_HEIGHT = 22;
	private static final int MAX_VISIBLE_SLOTS = 6;

	private static final int BG_COLOR = 0xCC000000;
	private static final int SLOT_COLOR = 0xFF2A2A3A;
	private static final int SLOT_HOVER_COLOR = 0xFF3A3A5A;
	private static final int SLOT_BORDER = 0xFF5391E1;
	private static final int PORTRAIT_BG = 0xFF1A1A2A;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final int LEVEL_COLOR = 0xFFAAAABB;
	private static final int TITLE_COLOR = 0xFF5391E1;
	private static final int TAB_ACTIVE_COLOR = 0xFF3A3A6A;
	private static final int TAB_INACTIVE_COLOR = 0xFF1A1A2A;
	private static final int TAB_DISABLED_COLOR = 0xFF111118;
	private static final int TAB_TEXT_ACTIVE = 0xFFFFFFFF;
	private static final int TAB_TEXT_INACTIVE = 0xFF888899;
	private static final int TAB_TEXT_DISABLED = 0xFF444455;
	private static final int NAV_BUTTON_COLOR = 0xFF2A2A3A;
	private static final int NAV_BUTTON_HOVER = 0xFF3A3A5A;
	private static final int LOADING_COLOR = 0xFF888899;

	private enum Tab { PARTY, PC }

	private final BlockPos seatPos;
	private final List<PartySlotData> partyData;
	private final int pcBoxCount;

	private Tab activeTab = Tab.PARTY;

	// Party portrait state
	private Object[] partyPortraitStates;
	private float[] partyBaseScales;
	private boolean portraitsAvailable;

	// PC state
	private int currentBoxIndex = 0;
	private String currentBoxName = "";
	private List<PartySlotData> currentBoxData;
	private Object[] pcPortraitStates;
	private float[] pcBaseScales;
	private boolean pcLoading = false;
	private int pcScrollOffset = 0;

	public CobblemonSeatScreen(BlockPos seatPos, List<PartySlotData> partyData, int pcBoxCount) {
		super(Lang.translateDirect("pokemon_seat.title"));
		this.seatPos = seatPos;
		this.partyData = partyData;
		this.pcBoxCount = pcBoxCount;
	}

	@Override
	protected void init() {
		int contentHeight = computeWindowHeight();
		setWindowSize(SLOT_WIDTH + PADDING * 2, contentHeight);
		super.init();

		portraitsAvailable = CobblemonPortraitRenderer.isAvailable();
		initPartyPortraits();
	}

	private void initPartyPortraits() {
		partyPortraitStates = new Object[partyData.size()];
		partyBaseScales = new float[partyData.size()];

		if (portraitsAvailable) {
			for (int i = 0; i < partyData.size(); i++) {
				PartySlotData slot = partyData.get(i);
				if (slot.present()) {
					partyPortraitStates[i] = CobblemonPortraitRenderer.createState(
							new HashSet<>(slot.aspects()));
					partyBaseScales[i] = CobblemonPortraitRenderer.getBaseScale(slot.speciesName());
				}
			}
		}
	}

	private void initPCPortraits() {
		if (currentBoxData == null)
			return;
		pcPortraitStates = new Object[currentBoxData.size()];
		pcBaseScales = new float[currentBoxData.size()];

		if (portraitsAvailable) {
			for (int i = 0; i < currentBoxData.size(); i++) {
				PartySlotData slot = currentBoxData.get(i);
				if (slot.present()) {
					pcPortraitStates[i] = CobblemonPortraitRenderer.createState(
							new HashSet<>(slot.aspects()));
					pcBaseScales[i] = CobblemonPortraitRenderer.getBaseScale(slot.speciesName());
				}
			}
		}
	}

	private int computeWindowHeight() {
		int tabSection = TAB_HEIGHT + TAB_GAP;
		int slotsHeight;

		if (activeTab == Tab.PARTY) {
			int presentCount = (int) partyData.stream().filter(PartySlotData::present).count();
			slotsHeight = Math.max(1, presentCount) * (SLOT_HEIGHT + SLOT_GAP) - SLOT_GAP;
		} else {
			slotsHeight = NAV_BAR_HEIGHT + SLOT_GAP
				+ MAX_VISIBLE_SLOTS * (SLOT_HEIGHT + SLOT_GAP) - SLOT_GAP;
		}

		return TITLE_HEIGHT + tabSection + slotsHeight + PADDING * 2;
	}

	public void receivePCBoxData(int boxIndex, String boxName, List<PartySlotData> boxData) {
		if (boxIndex != currentBoxIndex)
			return;
		this.currentBoxName = boxName;
		this.currentBoxData = boxData;
		this.pcLoading = false;
		this.pcScrollOffset = 0;
		initPCPortraits();
	}

	private void requestPCBox(int boxIndex) {
		this.currentBoxIndex = boxIndex;
		this.currentBoxData = null;
		this.pcPortraitStates = null;
		this.pcBaseScales = null;
		this.pcLoading = true;
		this.pcScrollOffset = 0;
		AllPackets.getChannel().sendToServer(new CobblemonSeatPCBoxRequestPacket(seatPos, boxIndex));
	}

	private void switchTab(Tab tab) {
		if (tab == activeTab)
			return;
		if (tab == Tab.PC && pcBoxCount <= 0)
			return;
		activeTab = tab;

		// Recalculate window size
		int contentHeight = computeWindowHeight();
		setWindowSize(SLOT_WIDTH + PADDING * 2, contentHeight);
		super.init();
		initPartyPortraits();

		if (tab == Tab.PC && currentBoxData == null && !pcLoading) {
			requestPCBox(0);
		}
	}

	// --- Rendering ---

	@Override
	protected void renderWindow(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
		int contentHeight = computeWindowHeight();

		graphics.fill(guiLeft, guiTop, guiLeft + windowWidth, guiTop + contentHeight, BG_COLOR);
		renderBorder(graphics, guiLeft, guiTop, windowWidth, contentHeight, SLOT_BORDER);

		graphics.drawCenteredString(font, title, guiLeft + windowWidth / 2, guiTop + PADDING, TITLE_COLOR);

		int y = guiTop + PADDING + TITLE_HEIGHT;

		// Render tabs
		y = renderTabs(graphics, mouseX, mouseY, y);
		y += TAB_GAP;

		// Render content
		if (activeTab == Tab.PARTY) {
			renderPartyContent(graphics, mouseX, mouseY, y, partialTicks);
		} else {
			renderPCContent(graphics, mouseX, mouseY, y, partialTicks);
		}
	}

	private int renderTabs(GuiGraphics graphics, int mouseX, int mouseY, int y) {
		int tabWidth = (SLOT_WIDTH - TAB_GAP) / 2;
		int partyTabX = guiLeft + PADDING;
		int pcTabX = partyTabX + tabWidth + TAB_GAP;

		// Party tab
		boolean partyActive = activeTab == Tab.PARTY;
		boolean partyHovered = mouseX >= partyTabX && mouseX < partyTabX + tabWidth
				&& mouseY >= y && mouseY < y + TAB_HEIGHT;
		int partyBg = partyActive ? TAB_ACTIVE_COLOR : TAB_INACTIVE_COLOR;
		int partyText = partyActive ? TAB_TEXT_ACTIVE : TAB_TEXT_INACTIVE;
		graphics.fill(partyTabX, y, partyTabX + tabWidth, y + TAB_HEIGHT, partyBg);
		if (partyActive || partyHovered)
			renderBorder(graphics, partyTabX, y, tabWidth, TAB_HEIGHT, SLOT_BORDER);
		graphics.drawCenteredString(font, "Party", partyTabX + tabWidth / 2, y + (TAB_HEIGHT - 8) / 2, partyText);

		// PC tab
		boolean pcDisabled = pcBoxCount <= 0;
		boolean pcActive = activeTab == Tab.PC;
		boolean pcHovered = !pcDisabled && mouseX >= pcTabX && mouseX < pcTabX + tabWidth
				&& mouseY >= y && mouseY < y + TAB_HEIGHT;
		int pcBg = pcDisabled ? TAB_DISABLED_COLOR : (pcActive ? TAB_ACTIVE_COLOR : TAB_INACTIVE_COLOR);
		int pcText = pcDisabled ? TAB_TEXT_DISABLED : (pcActive ? TAB_TEXT_ACTIVE : TAB_TEXT_INACTIVE);
		graphics.fill(pcTabX, y, pcTabX + tabWidth, y + TAB_HEIGHT, pcBg);
		if ((pcActive || pcHovered) && !pcDisabled)
			renderBorder(graphics, pcTabX, y, tabWidth, TAB_HEIGHT, SLOT_BORDER);
		graphics.drawCenteredString(font, "PC", pcTabX + tabWidth / 2, y + (TAB_HEIGHT - 8) / 2, pcText);

		return y + TAB_HEIGHT;
	}

	private void renderPartyContent(GuiGraphics graphics, int mouseX, int mouseY, int startY, float partialTicks) {
		int slotY = startY;
		for (int i = 0; i < partyData.size(); i++) {
			PartySlotData slot = partyData.get(i);
			if (!slot.present())
				continue;

			renderPokemonSlot(graphics, mouseX, mouseY, slotY, slot,
				partyPortraitStates != null && i < partyPortraitStates.length ? partyPortraitStates[i] : null,
				partyBaseScales != null && i < partyBaseScales.length ? partyBaseScales[i] : 1.0f,
				partialTicks);
			slotY += SLOT_HEIGHT + SLOT_GAP;
		}
	}

	private void renderPCContent(GuiGraphics graphics, int mouseX, int mouseY, int startY, float partialTicks) {
		// Navigation bar
		renderNavBar(graphics, mouseX, mouseY, startY);
		int slotStartY = startY + NAV_BAR_HEIGHT + SLOT_GAP;

		if (pcLoading) {
			graphics.drawCenteredString(font, "Loading...",
				guiLeft + windowWidth / 2, slotStartY + MAX_VISIBLE_SLOTS * (SLOT_HEIGHT + SLOT_GAP) / 2 - 4,
				LOADING_COLOR);
			return;
		}

		if (currentBoxData == null)
			return;

		// Collect present pokemon
		List<Integer> presentIndices = new ArrayList<>();
		for (int i = 0; i < currentBoxData.size(); i++) {
			if (currentBoxData.get(i).present())
				presentIndices.add(i);
		}

		if (presentIndices.isEmpty()) {
			graphics.drawCenteredString(font, "Empty",
				guiLeft + windowWidth / 2, slotStartY + MAX_VISIBLE_SLOTS * (SLOT_HEIGHT + SLOT_GAP) / 2 - 4,
				LOADING_COLOR);
			return;
		}

		// Render visible slots with scrolling
		int maxScroll = Math.max(0, presentIndices.size() - MAX_VISIBLE_SLOTS);
		pcScrollOffset = Math.min(pcScrollOffset, maxScroll);

		int slotY = slotStartY;
		int visibleCount = 0;
		for (int vi = pcScrollOffset; vi < presentIndices.size() && visibleCount < MAX_VISIBLE_SLOTS; vi++) {
			int dataIndex = presentIndices.get(vi);
			PartySlotData slot = currentBoxData.get(dataIndex);

			renderPokemonSlot(graphics, mouseX, mouseY, slotY, slot,
				pcPortraitStates != null && dataIndex < pcPortraitStates.length ? pcPortraitStates[dataIndex] : null,
				pcBaseScales != null && dataIndex < pcBaseScales.length ? pcBaseScales[dataIndex] : 1.0f,
				partialTicks);
			slotY += SLOT_HEIGHT + SLOT_GAP;
			visibleCount++;
		}

		// Scroll indicator
		if (presentIndices.size() > MAX_VISIBLE_SLOTS) {
			String scrollInfo = (pcScrollOffset + 1) + "-" + Math.min(pcScrollOffset + MAX_VISIBLE_SLOTS, presentIndices.size())
				+ " / " + presentIndices.size();
			graphics.drawCenteredString(font, scrollInfo,
				guiLeft + windowWidth / 2, slotY + 2, LEVEL_COLOR);
		}
	}

	private void renderNavBar(GuiGraphics graphics, int mouseX, int mouseY, int y) {
		int navX = guiLeft + PADDING;
		int buttonSize = NAV_BAR_HEIGHT;

		// Left arrow button
		boolean leftHovered = mouseX >= navX && mouseX < navX + buttonSize
				&& mouseY >= y && mouseY < y + buttonSize;
		graphics.fill(navX, y, navX + buttonSize, y + buttonSize,
			leftHovered ? NAV_BUTTON_HOVER : NAV_BUTTON_COLOR);
		if (leftHovered)
			renderBorder(graphics, navX, y, buttonSize, buttonSize, SLOT_BORDER);
		graphics.drawCenteredString(font, "<", navX + buttonSize / 2, y + (buttonSize - 8) / 2, TEXT_COLOR);

		// Right arrow button
		int rightX = navX + SLOT_WIDTH - buttonSize;
		boolean rightHovered = mouseX >= rightX && mouseX < rightX + buttonSize
				&& mouseY >= y && mouseY < y + buttonSize;
		graphics.fill(rightX, y, rightX + buttonSize, y + buttonSize,
			rightHovered ? NAV_BUTTON_HOVER : NAV_BUTTON_COLOR);
		if (rightHovered)
			renderBorder(graphics, rightX, y, buttonSize, buttonSize, SLOT_BORDER);
		graphics.drawCenteredString(font, ">", rightX + buttonSize / 2, y + (buttonSize - 8) / 2, TEXT_COLOR);

		// Box name in the center
		String displayName = currentBoxName.isEmpty() ? "Box " + (currentBoxIndex + 1) : currentBoxName;
		graphics.drawCenteredString(font, displayName,
			guiLeft + windowWidth / 2, y + (buttonSize - 8) / 2, TEXT_COLOR);
	}

	private void renderPokemonSlot(GuiGraphics graphics, int mouseX, int mouseY, int slotY,
			PartySlotData slot, Object portraitState, float baseScale, float partialTicks) {
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
		if (portraitsAvailable && portraitState != null) {
			graphics.enableScissor(portraitX, portraitY,
					portraitX + PORTRAIT_SIZE, portraitY + PORTRAIT_SIZE);
			CobblemonPortraitRenderer.renderPortrait(
					graphics, slot.speciesName(), portraitState,
					portraitX, portraitY, PORTRAIT_SIZE,
					baseScale, partialTicks);
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
	}

	// --- Input Handling ---

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		// Tab clicks
		int tabY = guiTop + PADDING + TITLE_HEIGHT;
		int tabWidth = (SLOT_WIDTH - TAB_GAP) / 2;
		int partyTabX = guiLeft + PADDING;
		int pcTabX = partyTabX + tabWidth + TAB_GAP;

		if (mouseY >= tabY && mouseY < tabY + TAB_HEIGHT) {
			if (mouseX >= partyTabX && mouseX < partyTabX + tabWidth) {
				switchTab(Tab.PARTY);
				return true;
			}
			if (mouseX >= pcTabX && mouseX < pcTabX + tabWidth) {
				switchTab(Tab.PC);
				return true;
			}
		}

		if (activeTab == Tab.PARTY) {
			return handlePartyClick(mouseX, mouseY) || super.mouseClicked(mouseX, mouseY, button);
		} else {
			return handlePCClick(mouseX, mouseY) || super.mouseClicked(mouseX, mouseY, button);
		}
	}

	private boolean handlePartyClick(double mouseX, double mouseY) {
		int slotY = guiTop + PADDING + TITLE_HEIGHT + TAB_HEIGHT + TAB_GAP;
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
		return false;
	}

	private boolean handlePCClick(double mouseX, double mouseY) {
		int navY = guiTop + PADDING + TITLE_HEIGHT + TAB_HEIGHT + TAB_GAP;
		int navX = guiLeft + PADDING;
		int buttonSize = NAV_BAR_HEIGHT;

		// Left arrow
		if (mouseX >= navX && mouseX < navX + buttonSize
				&& mouseY >= navY && mouseY < navY + buttonSize) {
			int newIndex = (currentBoxIndex - 1 + pcBoxCount) % pcBoxCount;
			requestPCBox(newIndex);
			return true;
		}

		// Right arrow
		int rightX = navX + SLOT_WIDTH - buttonSize;
		if (mouseX >= rightX && mouseX < rightX + buttonSize
				&& mouseY >= navY && mouseY < navY + buttonSize) {
			int newIndex = (currentBoxIndex + 1) % pcBoxCount;
			requestPCBox(newIndex);
			return true;
		}

		// Pokemon slot clicks
		if (currentBoxData == null || pcLoading)
			return false;

		List<Integer> presentIndices = new ArrayList<>();
		for (int i = 0; i < currentBoxData.size(); i++) {
			if (currentBoxData.get(i).present())
				presentIndices.add(i);
		}

		int slotStartY = navY + NAV_BAR_HEIGHT + SLOT_GAP;
		int slotY = slotStartY;
		int visibleCount = 0;
		for (int vi = pcScrollOffset; vi < presentIndices.size() && visibleCount < MAX_VISIBLE_SLOTS; vi++) {
			int dataIndex = presentIndices.get(vi);
			int slotX = guiLeft + PADDING;
			if (mouseX >= slotX && mouseX < slotX + SLOT_WIDTH
					&& mouseY >= slotY && mouseY < slotY + SLOT_HEIGHT) {
				AllPackets.getChannel().sendToServer(
					new CobblemonSeatPokemonPacket(seatPos, currentBoxIndex, dataIndex));
				onClose();
				return true;
			}
			slotY += SLOT_HEIGHT + SLOT_GAP;
			visibleCount++;
		}

		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (activeTab == Tab.PC && currentBoxData != null && !pcLoading) {
			List<Integer> presentIndices = new ArrayList<>();
			for (int i = 0; i < currentBoxData.size(); i++) {
				if (currentBoxData.get(i).present())
					presentIndices.add(i);
			}

			int maxScroll = Math.max(0, presentIndices.size() - MAX_VISIBLE_SLOTS);
			if (verticalAmount < 0) {
				pcScrollOffset = Math.min(pcScrollOffset + 1, maxScroll);
			} else if (verticalAmount > 0) {
				pcScrollOffset = Math.max(pcScrollOffset - 1, 0);
			}
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
	}

	// --- Utility ---

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
