package org.lovetropics.games.common.core.game.client_state.instance;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.world.item.ItemStack;
import org.lovetropics.games.common.core.game.client_state.GameClientState;
import org.lovetropics.games.common.core.game.client_state.GameClientStateType;
import org.lovetropics.games.common.core.game.client_state.GameClientStateTypes;

import java.util.List;
import java.util.Optional;

/// The modifiers of the game, as known by the player they are sent to
public record ModifiersClientState(List<Entry> entries) implements GameClientState {
	public static final MapCodec<ModifiersClientState> CODEC = Entry.CODEC.listOf().fieldOf("entries")
			.xmap(ModifiersClientState::new, ModifiersClientState::entries);

	@Override
	public GameClientStateType<?> getType() {
		return GameClientStateTypes.MODIFIERS.get();
	}

	/// @param icon        empty, like the name and description, until the player has found out what the modifier is
	/// @param name        empty until the player has found out what the modifier is
	/// @param description empty until the player has found out what the modifier is
	public record Entry(ItemStack icon, Optional<Component> name, Optional<Component> description, Component riddle, int level, int maxLevel) {
		public static final Codec<Entry> CODEC = RecordCodecBuilder.create(in -> in.group(
				ItemStack.OPTIONAL_CODEC.fieldOf("icon").forGetter(Entry::icon),
				ComponentSerialization.CODEC.optionalFieldOf("name").forGetter(Entry::name),
				ComponentSerialization.CODEC.optionalFieldOf("description").forGetter(Entry::description),
				ComponentSerialization.CODEC.fieldOf("riddle").forGetter(Entry::riddle),
				Codec.INT.fieldOf("level").forGetter(Entry::level),
				Codec.INT.fieldOf("max_level").forGetter(Entry::maxLevel)
		).apply(in, Entry::new));

		public boolean revealed() {
			return name.isPresent();
		}
	}
}
