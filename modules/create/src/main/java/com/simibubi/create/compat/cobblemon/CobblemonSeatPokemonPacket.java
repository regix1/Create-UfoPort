package com.simibubi.create.compat.cobblemon;

import com.simibubi.create.compat.Mods;
import com.simibubi.create.content.contraptions.actors.seat.SeatBlock;
import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public class CobblemonSeatPokemonPacket extends SimplePacketBase {

	private final BlockPos seatPos;
	private final int partySlot;

	public CobblemonSeatPokemonPacket(BlockPos seatPos, int partySlot) {
		this.seatPos = seatPos;
		this.partySlot = partySlot;
	}

	public CobblemonSeatPokemonPacket(RegistryFriendlyByteBuf buffer) {
		this.seatPos = buffer.readBlockPos();
		this.partySlot = buffer.readInt();
	}

	@Override
	public void write(RegistryFriendlyByteBuf buffer) {
		buffer.writeBlockPos(seatPos);
		buffer.writeInt(partySlot);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> {
			ServerPlayer player = context.getSender();
			if (player == null)
				return;

			if (player.distanceToSqr(seatPos.getX() + 0.5, seatPos.getY() + 0.5, seatPos.getZ() + 0.5) > 20 * 20)
				return;

			if (!(player.level().getBlockState(seatPos).getBlock() instanceof SeatBlock))
				return;

			if (SeatBlock.isSeatOccupied(player.level(), seatPos))
				return;

			if (partySlot < 0 || partySlot > 5)
				return;

			if (!Mods.COBBLEMON.isLoaded())
				return;

			CobblemonCompat.spawnAndSeatPokemon(player, seatPos, partySlot);
		});
		return true;
	}
}
