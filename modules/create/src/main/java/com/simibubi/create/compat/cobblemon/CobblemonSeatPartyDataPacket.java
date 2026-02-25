package com.simibubi.create.compat.cobblemon;

import java.util.ArrayList;
import java.util.List;

import com.simibubi.create.compat.cobblemon.CobblemonCompat.PartySlotData;
import com.simibubi.create.foundation.gui.ScreenOpener;
import com.simibubi.create.foundation.networking.SimplePacketBase;
import com.tterrag.registrate.fabric.EnvExecutor;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;

public class CobblemonSeatPartyDataPacket extends SimplePacketBase {

	private final BlockPos seatPos;
	private final List<PartySlotData> partyData;

	public CobblemonSeatPartyDataPacket(BlockPos seatPos, List<PartySlotData> partyData) {
		this.seatPos = seatPos;
		this.partyData = partyData;
	}

	public CobblemonSeatPartyDataPacket(RegistryFriendlyByteBuf buffer) {
		this.seatPos = buffer.readBlockPos();
		this.partyData = new ArrayList<>();
		for (int i = 0; i < 6; i++) {
			boolean present = buffer.readBoolean();
			if (present) {
				String speciesName = buffer.readUtf(64);
				int level = buffer.readInt();
				partyData.add(new PartySlotData(true, speciesName, level));
			} else {
				partyData.add(new PartySlotData(false, "", 0));
			}
		}
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(seatPos);
		for (int i = 0; i < 6; i++) {
			PartySlotData slot = i < partyData.size() ? partyData.get(i) : new PartySlotData(false, "", 0);
			buffer.writeBoolean(slot.present());
			if (slot.present()) {
				buffer.writeUtf(slot.speciesName(), 64);
				buffer.writeInt(slot.level());
			}
		}
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> EnvExecutor.runWhenOn(EnvType.CLIENT, () -> () -> openScreen()));
		return true;
	}

	@Environment(EnvType.CLIENT)
	private void openScreen() {
		ScreenOpener.open(new CobblemonSeatScreen(seatPos, partyData));
	}
}
