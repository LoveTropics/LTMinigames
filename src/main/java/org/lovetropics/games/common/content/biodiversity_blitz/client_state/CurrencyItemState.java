package org.lovetropics.games.common.content.biodiversity_blitz.client_state;

import org.lovetropics.games.common.content.biodiversity_blitz.BiodiversityBlitz;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

public record CurrencyItemState(ItemStack item) implements GameClientState {
	public static final MapCodec<CurrencyItemState> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
			ItemStack.CODEC.fieldOf("item").forGetter(CurrencyItemState::item)
	).apply(i, CurrencyItemState::new));

	@Override
	public GameClientStateType<?> getType() {
		return BiodiversityBlitz.CURRENCY_ITEM.get();
	}
}
