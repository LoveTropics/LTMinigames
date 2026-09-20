package org.lovetropics.games.common.content.river_race.client_state;

import org.lovetropics.games.common.content.river_race.RiverRace;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.DyeColor;

import java.util.List;

public record RiverRaceClientBarState(
		Team topTeam,
		Team bottomTeam,
		List<Zone> lockedZones
) implements GameClientState {
	public static final MapCodec<RiverRaceClientBarState> CODEC = MapCodec.unit(() -> null);
	public static final StreamCodec<ByteBuf, RiverRaceClientBarState> STREAM_CODEC = StreamCodec.composite(
			Team.STREAM_CODEC, RiverRaceClientBarState::topTeam,
			Team.STREAM_CODEC, RiverRaceClientBarState::bottomTeam,
			Zone.STREAM_CODEC.apply(ByteBufCodecs.list()), RiverRaceClientBarState::lockedZones,
			RiverRaceClientBarState::new
	);

	@Override
	public GameClientStateType<?> getType() {
		return RiverRace.BAR_STATE.get();
	}

	public record Team(
			int color,
			int progress,
			IntList players
	) {
		public static final StreamCodec<ByteBuf, Team> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.RGB_COLOR, Team::color,
				ByteBufCodecs.VAR_INT, Team::progress,
				ByteBufCodecs.collection(IntArrayList::new, ByteBufCodecs.VAR_INT), Team::players,
				Team::new
		);
	}

	public record Zone(int start, int length, DyeColor color) {
		public static final StreamCodec<ByteBuf, Zone> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, Zone::start,
				ByteBufCodecs.VAR_INT, Zone::length,
				DyeColor.STREAM_CODEC, Zone::color,
				Zone::new
		);
	}
}
