package com.lovetropics.minigames.common.core.game.client_state;

import com.lovetropics.minigames.LoveTropics;
import com.lovetropics.minigames.common.core.game.client_state.instance.BeaconClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.CollidersClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.CraftingBeeCraftsClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.DisableBobbingState;
import com.lovetropics.minigames.common.core.game.client_state.instance.DisablePlayerMovementState;
import com.lovetropics.minigames.common.core.game.client_state.instance.FogClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.GlowTeamMembersState;
import com.lovetropics.minigames.common.core.game.client_state.instance.HealthTagClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.HideNameTagsState;
import com.lovetropics.minigames.common.core.game.client_state.instance.HideRecipeBookClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.InvertControlsClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.PointTagClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.ReplaceTexturesClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.ResourcePackClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.SidebarClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.SpectatingClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.SwapMovementClientState;
import com.lovetropics.minigames.common.core.game.client_state.instance.TeamMembersClientState;
import com.lovetropics.minigames.common.util.registry.GameClientTweakEntry;
import com.lovetropics.minigames.common.util.registry.LoveTropicsRegistrate;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class GameClientStateTypes {
	public static final ResourceKey<Registry<GameClientStateType<?>>> REGISTRY_KEY = ResourceKey.createRegistryKey(LoveTropics.location("game_client_state"));

	public static final DeferredRegister<GameClientStateType<?>> REGISTER = DeferredRegister.create(REGISTRY_KEY, LoveTropics.ID);

	public static final Registry<GameClientStateType<?>> REGISTRY = REGISTER.makeRegistry(builder -> builder.sync(true));

	private static final LoveTropicsRegistrate REGISTRATE = LoveTropics.registrate();

	public static final Codec<GameClientStateType<?>> TYPE_CODEC = Codec.lazyInitialized(REGISTRY::byNameCodec);

	public static final GameClientTweakEntry<ReplaceTexturesClientState> REPLACE_TEXTURES = register("replace_textures", ReplaceTexturesClientState.CODEC);
	public static final GameClientTweakEntry<SpectatingClientState> SPECTATING = register("spectating", SpectatingClientState.CODEC);
	public static final GameClientTweakEntry<ResourcePackClientState> RESOURCE_PACK = register("resource_pack", ResourcePackClientState.CODEC);
	public static final GameClientTweakEntry<HealthTagClientState> HEALTH_TAG = register("health_tag", HealthTagClientState.CODEC);
	public static final GameClientTweakEntry<SidebarClientState> SIDEBAR = register("sidebar", SidebarClientState.CODEC);
	public static final GameClientTweakEntry<BeaconClientState> BEACON = register("beacon", BeaconClientState.CODEC);
	public static final GameClientTweakEntry<FogClientState> FOG = register("fog", FogClientState.CODEC);
	public static final GameClientTweakEntry<TeamMembersClientState> TEAM_MEMBERS = register("team_members", TeamMembersClientState.CODEC);
	public static final GameClientTweakEntry<GlowTeamMembersState> GLOW_TEAM_MEMBERS = register("glow_team_members", MapCodec.unit(GlowTeamMembersState.INSTANCE), StreamCodec.unit(GlowTeamMembersState.INSTANCE));
	public static final GameClientTweakEntry<PointTagClientState> POINT_TAGS = register("point_tags", PointTagClientState.CODEC);
	public static final GameClientTweakEntry<HideRecipeBookClientState> HIDE_RECIPE_BOOK = register("hide_recipe_book", HideRecipeBookClientState.CODEC);
	public static final GameClientTweakEntry<CraftingBeeCraftsClientState> CRAFTING_BEE_CRAFTS = register("crafting_bee_crafts", CraftingBeeCraftsClientState.CODEC);
	public static final GameClientTweakEntry<InvertControlsClientState> INVERT_CONTROLS = register("invert_controls", InvertControlsClientState.CODEC);
	public static final GameClientTweakEntry<SwapMovementClientState> SWAP_MOVEMENT = register("swap_movement", SwapMovementClientState.CODEC);
	public static final GameClientTweakEntry<CollidersClientState> COLLIDERS = register("colliders", CollidersClientState.CODEC, CollidersClientState.STREAM_CODEC);
	public static final GameClientTweakEntry<HideNameTagsState> HIDE_NAME_TAGS = register("hide_name_tags", MapCodec.unit(HideNameTagsState.INSTANCE), StreamCodec.unit(HideNameTagsState.INSTANCE));
	public static final GameClientTweakEntry<DisableBobbingState> DISABLE_BOBBING = register("disable_bobbing", MapCodec.unit(DisableBobbingState.INSTANCE), StreamCodec.unit(DisableBobbingState.INSTANCE));
	public static final GameClientTweakEntry<DisablePlayerMovementState> DISABLE_PLAYER_MOVEMENT = register("disable_player_movement", MapCodec.unit(DisablePlayerMovementState.INSTANCE), StreamCodec.unit(DisablePlayerMovementState.INSTANCE));

	public static <T extends GameClientState> GameClientTweakEntry<T> register(final String name, final MapCodec<T> codec) {
		return REGISTRATE.object(name)
				.clientState(codec)
				.register();
	}

	public static <T extends GameClientState> GameClientTweakEntry<T> register(final String name, final MapCodec<T> codec, final StreamCodec<? super RegistryFriendlyByteBuf, T> streamCodec) {
		return REGISTRATE.object(name)
				.clientState(codec).streamCodec(streamCodec)
				.register();
	}

	public static void init(IEventBus modBus) {
		REGISTER.register(modBus);
	}
}
